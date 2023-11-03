(ns renderer.document.events
  (:require
   [re-frame.core :as rf]
   [re-frame.interceptor :refer [->interceptor get-effect get-coeffect assoc-coeffect assoc-effect]]
   [renderer.utils.uuid :as uuid]
   [renderer.utils.vec :as vec]
   [renderer.document.db :as db]
   [renderer.history.handlers :as history]
   [renderer.element.handlers :as element-handlers]
   [renderer.document.handlers :as handlers]
   [renderer.frame.handlers :as frame]))

(def active-document-path
  (let [db-store-key :re-frame-path/db-store]
    (->interceptor
     :id :active-document-path
     :before (fn
               [context]
               (let [original-db (get-coeffect context :db)]
                 (-> context
                     (update db-store-key conj original-db)
                     (assoc-coeffect :db (get-in original-db [:documents (:active-document original-db)])))))
     :after (fn [context]
              (let [db-store (db-store-key context)
                    original-db (peek db-store)
                    new-db-store (pop db-store)
                    context' (-> (assoc context db-store-key new-db-store)
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

#_:clj-kondo/ignore
(rf/reg-event-db
 :document/toggle-snap
 active-document-path
 (fn [db [_]]
   (update db :snap? not)))

(rf/reg-event-db
 :document/toggle-xml
 active-document-path
 (fn [db [_]]
   (update db :xml? not)))

#_:clj-kondo/ignore
(rf/reg-event-db
 :document/toggle-history
 active-document-path
 (fn [db [_]]
   (update db :history? not)))

(rf/reg-event-db
 :document/set-fill
 (fn [{active-document :active-document :as db} [_ fill]]
   (-> db
       (assoc-in [:documents active-document :fill] fill)
       (element-handlers/set-attribute :fill fill))))

(rf/reg-event-db
 :document/set-stroke
 (fn [{active-document :active-document :as db} [_ stroke]]
   (-> db
       (assoc-in [:documents active-document :stroke] stroke)
       (element-handlers/set-attribute :stroke stroke))))

(rf/reg-event-db
 :document/new
 (fn [db [_]]
   (let [key (uuid/generate)
         title (str "Untitled-" (inc (count (:documents db))))
         document-tabs (:document-tabs db)
         active-index (.indexOf document-tabs (:active-document db))]
     (-> db
         (assoc-in [:documents key] (merge
                                     db/default-document
                                     {:key key :title title}))
         (update :document-tabs #(vec/add % (inc active-index) key))
         (assoc :active-document key)
         (frame/pan-to-element (:active-page db/default-document))
         (history/init "Create document")))))

(rf/reg-event-db
 :document/close
 (fn [db [_ key]]
   (handlers/close db key)))

(rf/reg-event-db
 :document/close-active
 (fn [db [_]]
   (handlers/close db)))

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
     (if (and (nat-int? scrolled-index)
              (< scrolled-index (count document-tabs)))
       (assoc db :active-document (get document-tabs scrolled-index))
       db))))

(rf/reg-event-db
 :document/swap-position
 (fn [db [_ dragged-key swapped-key]]
   (let [document-tabs (:document-tabs db)
         dragged-index (.indexOf document-tabs dragged-key)
         swapped-index (.indexOf document-tabs swapped-key)]
     (assoc db :document-tabs (vec/swap document-tabs dragged-index swapped-index)))))
