(ns renderer.document.events
  (:require
   [de-dupe.core :as dd]
   [re-frame.core :as rf]
   [re-frame.interceptor :refer [->interceptor get-effect get-coeffect assoc-coeffect assoc-effect]]
   [renderer.document.db :as db]
   [renderer.document.handlers :as h]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h]
   [renderer.utils.uuid :as uuid]
   [renderer.utils.vec :as vec]))

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
                (cond-> context'
                  (not= db ::not-found)
                  (assoc-effect :db (assoc-in original-db [:documents (:active-document original-db)] db))))))))

(rf/reg-event-db
 :document/set-hovered-keys
 active-document-path
 (fn [db [_ ks]]
   (assoc db :hovered-keys ks)))

(rf/reg-event-db
 :document/set-filter
 active-document-path
 (fn [db [_ ks]]
   (assoc db :filter ks)))

(rf/reg-event-db
 :document/set-temp-element
 active-document-path
 (fn [db [_ ks]]
   (assoc db :temp-element ks)))

(rf/reg-event-db
 :document/swap-colors
 active-document-path
 (fn [db [_]]
   (assoc db
          :fill (:stroke db)
          :stroke (:fill db))))

(rf/reg-event-db
 :document/set-fill
 (fn [{active-document :active-document :as db} [_ fill]]
   (-> db
       (assoc-in [:documents active-document :fill] fill)
       (element.h/set-attr :fill fill))))

(rf/reg-event-db
 :document/set-stroke
 (fn [{active-document :active-document :as db} [_ stroke]]
   (-> db
       (assoc-in [:documents active-document :stroke] stroke)
       (element.h/set-attr :stroke stroke))))

(rf/reg-event-db
 :document/close
 (fn [db [_ key]]
   (h/close db key)))

(rf/reg-event-db
 :document/close-active
 (fn [db [_]]
   (h/close db)))

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

(rf/reg-event-fx
 :document/new
 (fn [{:keys [db]} [_]]
   {:db (let [key (uuid/generate)
              title (str "Untitled-" (inc (count (:documents db))))
              document-tabs (:document-tabs db)
              active-index (.indexOf document-tabs (:active-document db))
              document (merge db/default-document {:key key
                                                   :title title})]
          (-> db
              (assoc-in [:documents key] document)
              (update :document-tabs #(vec/add % (inc active-index) key))
              (assoc :active-document key)
              (history.h/init "Create document")))
    :dispatch [:pan-to-element :default-page]}))

(rf/reg-event-fx
 :document/open
 (fn [_ [_]]
   {:send-to-main {:action "openDocument"}}))

(rf/reg-event-fx
 :document/save
 (fn [{:keys [db]} [_]]
   (let [document (get-in db [:documents (:active-document db)])
         duped (update-in document [:history] dd/de-dupe)]
     {:send-to-main {:action "saveDocument" :data (pr-str duped)}})))

(rf/reg-event-fx
 :document/save-as
 (fn [{:keys [db]} [_]]
   db))

(rf/reg-event-fx
 :document/save-all
 (fn [{:keys [db]} [_]]
   db))
