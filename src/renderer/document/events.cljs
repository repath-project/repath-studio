(ns renderer.document.events
  (:require
   [clojure.edn :as edn]
   #_[de-dupe.core :as dd]
   [re-frame.core :as rf]
   [re-frame.interceptor :refer [->interceptor get-effect get-coeffect assoc-coeffect assoc-effect]]
   [renderer.document.db :as db]
   [renderer.document.handlers :as h]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h]
   [renderer.utils.vec :as vec]))

(def active-document-path
  (let [db-store-key :re-frame-path/db-store]
    (->interceptor
     :id :active-document-path
     :before (fn [context]
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
 :document/collapse-el
 active-document-path
 (fn [db [_ el-k]]
   (update db :collapsed-keys conj el-k)))

(rf/reg-event-db
 :document/expand-el
 active-document-path
 (fn [db [_ el-k]]
   (update db :collapsed-keys disj el-k)))

(rf/reg-event-db
 :document/set-filter
 active-document-path
 (fn [db [_ ks]]
   (assoc db :filter ks)))

(rf/reg-event-db
 :document/set-temp-element
 active-document-path
 (fn [db [_ el]]
   (assoc db :temp-element el)))

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
   {:db (-> db
            (h/create-tab db/default-document)
            (element.h/create {:tag :svg
                               :attrs {:width "800" :height "600"}})
            element.h/deselect
            (history.h/finalize "Create document"))
    :dispatch [:center]}))

(rf/reg-event-fx
 :document/open
 (fn [_ [_]]
   {:send-to-main {:action "openDocument"}}))

(rf/reg-event-fx
 :document/load
 (fn [{:keys [db]} [_ data]]
   (let [document (-> (.-data data)
                      edn/read-string
                      (assoc :path (.-path data)
                             :title (.-title data))
                      ;; FIXME: Still contains cached values after expand.
                      #_(update-in [:history :states] dd/expand))]
     {:db (-> db
              (h/create-tab document)
              (history.h/finalize "Load document")
              (update-in [:documents (:key document)] #(assoc % :save (-> % :history :position))))
      :dispatch [:center]})))

(rf/reg-event-fx
 :document/save
 (fn [{:keys [db]} [_]]
   (let [document (-> db
                      (get-in [:documents (:active-document db)])
                      (assoc :save (history.h/current-position db))
                      (dissoc :history)
                      #_(update-in [:history :states] dd/de-dupe-eq))]
     {:send-to-main {:action "saveDocument" :data (pr-str document)}})))

(rf/reg-event-fx
 :document/save-as
 (fn [{:keys [db]} [_]]
   (let [document (-> db
                      (get-in [:documents (:active-document db)])
                      (assoc :save (history.h/current-position db))
                      (dissoc :history :path))]
     {:send-to-main {:action "saveDocument" :data (pr-str document)}})))

(rf/reg-event-db
 :document/saved
 (fn [db [_ data]]
   (let [document (-> (.-data data) edn/read-string)
         key (:key document)]
     ;; Update the path/name and the saved position of the document.
     ;; Any other changes that could happen while saving should be preserved.
     (-> db
         (assoc-in [:documents key :path] (.-path data))
         (assoc-in [:documents key :title] (.-title data))
         (assoc-in [:documents key :save] (:save document))))))

(rf/reg-event-fx
 :document/save-all
 (fn [{:keys [db]} [_]]
   db))
