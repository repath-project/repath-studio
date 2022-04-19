(ns repath.studio.documents.events
  (:require
   [re-frame.core :as rf]
   [re-frame.interceptor :refer [->interceptor get-effect get-coeffect assoc-coeffect assoc-effect]]
   [repath.studio.helpers :as helpers]
   [repath.studio.documents.db :as db]
   [repath.studio.history.handlers :as history]
   [repath.studio.canvas-frame.handlers :as canvas-frame]
   [repath.studio.tools.base :as tools]
   [de-dupe.core :as dd]
   [repath.studio.documents.handlers :as handlers]))

(def active-document-path
  (let [db-store-key :re-frame-path/db-store]
    (->interceptor
     :id     :active-document-path
     :before (fn
               [context]
               (let [original-db (get-coeffect context :db)]
                 (-> context
                     (update db-store-key conj original-db)
                     (assoc-coeffect :db (get-in original-db [:documents (:active-document original-db)])))))
     :after  (fn [context]
               (let [db-store     (db-store-key context)
                     original-db  (peek db-store)
                     new-db-store (pop db-store)
                     context'     (-> (assoc context db-store-key new-db-store)
                                      (assoc-coeffect :db original-db))
                     db           (get-effect context :db ::not-found)]
                 (if (= db ::not-found)
                   context'
                   (->> (assoc-in original-db [:documents (:active-document original-db)] db)
                        (assoc-effect context' :db))))))))

(rf/reg-event-db
 :document/set-hovered-keys
 active-document-path
 (fn [db [_ keys]]
   (assoc db :hovered-keys keys)))

(rf/reg-event-db
 :document/set-filter
 active-document-path
 (fn [db [_ keys]]
   (assoc db :filter keys)))

(rf/reg-event-db
 :document/set-temp-element
 active-document-path
 (fn [db [_ keys]]
   (assoc db :temp-element keys)))

(rf/reg-event-db
 :document/swap-colors
 active-document-path
 (fn [db [_]]
   (assoc db
          :fill (:stroke db)
          :stroke (:fill db))))

(rf/reg-event-db
 :document/toggle-rulers
 active-document-path
 (fn [db [_]]
   (update db :rulers? not)))

(rf/reg-event-db
 :document/toggle-grid
 active-document-path
 (fn [db [_]]
   (update db :grid? not)))

(rf/reg-event-db
 :document/set-fill
 (fn [db [_ fill]]
   (-> db
       (handlers/set-fill fill)
       (history/finalize (str "Set fill " (tools/rgba fill))))))

(rf/reg-event-fx
 :document/open
 (fn [_ [_]]
   (.then (js/window.api.send "toMain" #js {:action "openDocument"}))))

(rf/reg-event-db
 :document/save
 (fn [{active-document :active-document :as db} [_]]
   (let [document (get-in db [:documents active-document])
         duped (assoc document :history (dd/de-dupe (:history document)))]
     (.then (js/window.api.send "toMain" #js {:action "saveDocument" :data (pr-str duped)})))))

(rf/reg-event-db
 :document/new
 (fn [db [_]]
   (let [key (helpers/uid)
         title (str "Untitled-" (inc (count (:documents db))))]
     (-> db
         (assoc-in [:documents key] (merge db/default-document {:key key :title title}))
         (update :document-tabs conj key)
         (assoc :active-document key)
         (canvas-frame/pan-to-element (:active-page db/default-document))
         (history/init "Create document")))))

(rf/reg-event-db
 :document/close
 (fn [{active-document :active-document :as db} [_ key]]
   (let [document-tabs (:document-tabs db)
         index (.indexOf document-tabs key)
         active-document (if (= active-document key) (get document-tabs (if (= index 0) (inc index) (dec index))) active-document)]
    (-> db
        (update :document-tabs #(filterv (complement #{key}) %))
        (assoc :active-document active-document)
        (update :documents dissoc key)))))

(rf/reg-event-db
 :document/close-others
 (fn [db [_ key]]
   (-> db
       (assoc :document-tabs [key]
              :active-document key)
       (assoc-in [:documents key] (get-in db [:documents key])))))

(rf/reg-event-db
 :document/close-all
 (fn [db [_]]
     (-> db
         (assoc :document-tabs [])
         (dissoc :active-document :documents))))

(rf/reg-event-db
 :document/scroll
 (fn [db [_ direction]]
   (let [document-tabs (:document-tabs db)
         index (.indexOf document-tabs (:active-document db))
         scrolled-index (if (pos? direction) (inc index) (dec index))]
     (if (and (nat-int? scrolled-index) (< scrolled-index (count document-tabs))) (assoc db :active-document (get document-tabs scrolled-index)) db))))

(rf/reg-event-db
 :document/swap-position
 (fn [db [_ dragged-key swapped-key]]
   (let [document-tabs (:document-tabs db)
         dragged-index (.indexOf document-tabs dragged-key)
         swapped-index (.indexOf document-tabs swapped-key)]
     (assoc db :document-tabs (helpers/vec-swap document-tabs dragged-index swapped-index)))))
