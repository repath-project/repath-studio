(ns renderer.document.events
  (:require
   #_[de-dupe.core :as dd]
   [platform]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [re-frame.interceptor :refer [->interceptor get-effect get-coeffect assoc-coeffect assoc-effect]]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.document.db :as db]
   [renderer.document.handlers :as h]
   [renderer.element.handlers :as element.h]
   [renderer.frame.events :as-alias frame.e]
   [renderer.history.handlers :as history.h]
   [renderer.utils.file :as file]
   [renderer.utils.local-storage :as local-storage]
   [renderer.utils.uuid :as uuid]
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
                    db (get-effect context :db ::not-found)]
                (cond-> context'
                  (not= db ::not-found)
                  (assoc-effect :db (assoc-in original-db [:documents (:active-document original-db)] db))))))))

(rf/reg-event-db
 ::set-hovered-keys
 active-document-path
 (fn [db [_ ks]]
   (assoc db :hovered-keys ks)))

(rf/reg-event-db
 ::collapse-el
 active-document-path
 (fn [db [_ k]]
   (update db :collapsed-keys conj k)))

(rf/reg-event-db
 ::expand-el
 active-document-path
 (fn [db [_ k]]
   (update db :collapsed-keys disj k)))

(rf/reg-event-db
 ::toggle-filter
 active-document-path
 (fn [db [_ k]]
   (if (= (:filter db) k)
     (dissoc db :filter)
     (assoc db :filter k))))

(rf/reg-event-db
 ::set-temp-element
 active-document-path
 (fn [db [_ el]]
   (assoc db :temp-element el)))

(rf/reg-event-db
 ::swap-colors
 active-document-path
 (fn [db [_]]
   (assoc db
          :fill (:stroke db)
          :stroke (:fill db))))

(rf/reg-event-db
 ::set-fill
 (fn [{active-document :active-document :as db} [_ fill]]
   (-> db
       (assoc-in [:documents active-document :fill] fill)
       (element.h/set-attr :fill fill))))

(rf/reg-event-db
 ::set-stroke
 (fn [{active-document :active-document :as db} [_ stroke]]
   (-> db
       (assoc-in [:documents active-document :stroke] stroke)
       (element.h/set-attr :stroke stroke))))

(rf/reg-event-fx
 ::close
 (fn [{:keys [db]} [_ k confirm?]]
   (if (or (h/saved? db k) (not confirm?))
     {:db (h/close db k)}
     {:fx [[:dispatch [::set-active k]]
           [:dispatch [::dialog.e/save k]]]})))

(rf/reg-event-fx
 ::close-active
 (fn [{:keys [db]} [_]]
   (let [active-document (:active-document db)]
     (if (h/saved? db active-document)
       {:db (h/close db active-document)}
       {:dispatch [::dialog.e/save active-document]}))))

(rf/reg-event-db
 ::close-saved
 (fn [db [_]]
   (let [saved (filter #(h/saved? db %) (:document-tabs db))]
     (reduce h/close db saved))))

(rf/reg-event-fx
 ::close-others
 (fn [{:keys [db]} [_ k]]
   (let [to-be-closed (disj (set (keys (:documents db))) k)]
     {:fx (mapv (fn [k] [:dispatch [::close k true]]) to-be-closed)})))

(rf/reg-event-fx
 ::close-all
 (fn [{:keys [db]} [_]]
   {:fx (mapv (fn [k] [:dispatch [::close k true]]) (keys (:documents db)))}))

(rf/reg-event-db
 ::scroll
 (fn [db [_ direction]]
   (let [document-tabs (:document-tabs db)
         index (.indexOf document-tabs (:active-document db))
         scrolled-index (if (pos? direction) (inc index) (dec index))]
     (cond-> db
       (and (nat-int? scrolled-index)
            (< scrolled-index (count document-tabs)))
       (h/set-active (get document-tabs scrolled-index))))))

(rf/reg-event-db
 ::swap-position
 (fn [db [_ dragged-key swapped-key]]
   (let [document-tabs (:document-tabs db)
         dragged-index (.indexOf document-tabs dragged-key)
         swapped-index (.indexOf document-tabs swapped-key)]
     (assoc db :document-tabs (vec/swap document-tabs dragged-index swapped-index)))))

(rf/reg-event-fx
 ::new
 (fn [{:keys [db]} [_]]
   {:db (-> db
            (h/create-tab db/default-document)
            (element.h/create {:tag :svg
                               :attrs {:width "800" :height "600"}})
            (history.h/finalize "Create document"))
    :fx [[:dispatch [::frame.e/center]]
         [:focus nil]]}))

(def file-picker-options
  {:startIn "documents"
   :types [{:accept {"application/repath.studio" [".rps"]}}]})

(rf/reg-fx
 ::open
 (fn []
   (file/open! file-picker-options)))

(rf/reg-event-fx
 ::open
 (fn [_ [_ file-path]]
   (if platform/electron?
     {:send-to-main {:action "openDocument" :data file-path}}
     {::open nil})))

(rf/reg-event-fx
 ::open-directory
 (fn [_ [_ path]]
   {:send-to-main {:action "openDirectory" :data path}}))

(rf/reg-event-fx
 ::load
 local-storage/persist
 (fn [{:keys [db]} [_ document]]
   (let [open-key (h/search-by-path db (:path document))
         document (-> document
                      (assoc :key (or open-key (uuid/generate)))
                      ;; FIXME: Contains cached values after expand.
                      #_(update-in document [:history :states] dd/expand))]
     {:db (cond-> db
            (not open-key)
            (-> (h/create-tab document)
                (history.h/finalize "Load document")
                (update-in [:documents (:key document)]
                           #(assoc % :save (-> % :history :position))))

            :always
            (-> (h/add-recent (:path document))
                (h/set-active (:key document))))
      :fx [[:dispatch [::frame.e/center]]
           [:focus nil]]})))

(rf/reg-fx
 ::save-as
 (fn [data]
   (file/save!
    file-picker-options
    (fn [^js/FileSystemFileHandle file-handle]
      (p/let [writable (.createWritable file-handle)]
        (.then (.write writable (pr-str (dissoc data :closing? :path)))
               (let [document (assoc data :title (.-name file-handle))]
                 (.close writable)
                 (rf/dispatch [::saved document]))))))))

(defn save-format
  [db]
  (-> db
      (get-in [:documents (:active-document db)])
      (assoc :save (history.h/current-position db))
      #_(update-in [:history :states] dd/de-dupe-eq)
      (dissoc :history)))

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} [_]]
   (let [document (save-format db)]
     (if platform/electron?
       {:send-to-main {:action "saveDocument" :data (pr-str document)}}
       ;; The path is not available when we use web APIs for security reasons,
       ;; so we always dispatch save-as.
       {::save-as document}))))

(rf/reg-fx
 ::download
 (fn [data]
   (file/download! data)))

(rf/reg-event-fx
 ::download
 (fn [{:keys [db]} [_]]
   (let [document (save-format db)]
     {::download (pr-str document)})))

(rf/reg-event-fx
 ::save-and-close
 (fn [{:keys [db]} [_]]
   (let [document (-> (save-format db)
                      (assoc :closing? true))]
     (if platform/electron?
       {:send-to-main {:action "saveDocument" :data (pr-str document)}}
       {::save-as document}))))

(rf/reg-event-fx
 ::save-as
 (fn [{:keys [db]} [_]]
   (let [document (save-format db)]
     (if platform/electron?
       {:send-to-main {:action "saveDocumentAs" :data (pr-str document)}}
       {::save-as document}))))

(rf/reg-event-db
 ::saved
 (fn [db [_ document]]
   ;; Update the path, the title and the saved position of the document.
   ;; Any other changes that could happen while saving should be preserved.
   (let [updates (select-keys document [:path :title :save])]
     (cond-> db
       :always
       (h/add-recent (:path updates))

       (:closing? document)
       (h/close (:key document))

       (not (:closing? document))
       (update-in [:documents (:key document)] merge updates)))))

(rf/reg-event-db
 ::clear-recent
 local-storage/persist
 (fn [db [_]]
   (assoc db :recent [])))

(rf/reg-event-db
 ::set-active
 (fn [db [_ k]]
   (h/set-active db k)))
