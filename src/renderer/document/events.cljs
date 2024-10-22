(ns renderer.document.events
  (:require
   [cljs.reader :as edn]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx :refer [persist]]
   [renderer.app.events :as-alias app.e]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.dialog.handlers :as dialog.h]
   [renderer.dialog.views :as dialog.v]
   [renderer.document.db :as db]
   [renderer.document.handlers :as h]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h :refer [finalize]]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.utils.compatibility :as compatibility]
   [renderer.utils.dom :as dom]
   [renderer.utils.system :as system]
   [renderer.utils.vec :as vec]
   [renderer.window.effects :as-alias window.fx]))

(def file-picker-options
  {:startIn "documents"
   :types [{:accept {"application/repath-studio" [".rps"]}}]})

(rf/reg-event-db
 ::center
 [persist]
 h/center)

(rf/reg-event-db
 ::set-hovered-id
 (fn [db [_ id]]
   (h/set-hovered-ids db #{id})))

(rf/reg-event-db
 ::clear-hovered
 (fn [db [_]]
   (h/set-hovered-ids db #{})))

(rf/reg-event-db
 ::collapse-el
 [persist]
 (fn [db [_ id]]
   (h/collapse-el db id)))

(rf/reg-event-db
 ::expand-el
 [persist]
 (fn [db [_ id]]
   (h/expand-el db id)))

(rf/reg-event-db
 ::toggle-filter
 [persist]
 (fn [db [_ id]]
   (if (= (:filter (h/active db)) id)
     (update-in db [:documents (:active-document db)] dissoc :filter)
     (assoc-in db [:documents (:active-document db) :filter] id))))

(rf/reg-event-db
 ::swap-colors
 [persist]
 (fn [db [_]]
   (-> db
       (h/assoc-attr :fill (h/attr db :stroke))
       (h/assoc-attr :stroke (h/attr db :fill)))))

(rf/reg-event-db
 ::set-attr
 (fn [db [_ k v]]
   (h/assoc-attr db k v)))

(rf/reg-event-db
 ::close
 [persist]
 (fn [db [_ id confirm?]]
   (if (or (h/saved? db id) (not confirm?))
     (h/close db id)
     (-> db
         (h/set-active id)
         (dialog.h/create {:title "Do you want to save your changes?"
                           :close-button true
                           :content (dialog.v/save (get-in db [:documents id]))
                           :attrs {:onOpenAutoFocus dom/prevent-default!}})))))

(rf/reg-event-fx
 ::close-active
 [persist]
 (fn [{:keys [db]} [_]]
   {:dispatch [::close (:active-document db) true]}))

(rf/reg-event-db
 ::close-all-saved
 [persist]
 (fn [db [_]]
   (reduce h/close db (h/saved-ids db))))

(rf/reg-event-fx
 ::close-others
 (fn [{:keys [db]} [_ id]]
   (let [to-be-closed (disj (apply sorted-set (:document-tabs db)) id)]
     {:dispatch-n (mapv (fn [id] [::close id true]) to-be-closed)})))

(rf/reg-event-fx
 ::close-all
 (fn [{:keys [db]} [_]]
   {:db (h/set-active db (last (:document-tabs db)))
    :fx (mapv (fn [id] [:dispatch [::close id true]]) (:document-tabs db))}))

(rf/reg-event-db
 ::scroll
 [persist]
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
 [persist]
 (fn [db [_ dragged-key swapped-key]]
   (let [document-tabs (:document-tabs db)
         dragged-index (.indexOf document-tabs dragged-key)
         swapped-index (.indexOf document-tabs swapped-key)]
     (assoc db :document-tabs (vec/swap document-tabs dragged-index swapped-index)))))

(defn- create
  ([db, guid]
   (create db guid [595 842]))
  ([db guid size]
   (-> db
       (h/create-tab (assoc db/default :id guid))
       (element.h/create-default-canvas size)
       (h/center))))

(rf/reg-event-fx
 ::new
 [(rf/inject-cofx ::app.fx/guid)
  (finalize "Create document")]
 (fn [{:keys [db guid]} [_]]
   {:db (create db guid)}))

(rf/reg-event-fx
 ::init
 [(rf/inject-cofx ::app.fx/guid)
  (finalize "Init document")]
 (fn [{:keys [db guid]} [_]]
   {:db (cond-> db
          (not (:active-document db))
          (create guid))}))

(rf/reg-event-fx
 ::new-from-template
 [(rf/inject-cofx ::app.fx/guid)
  (finalize "Create document from template")]
 (fn [{:keys [db guid]} [_ size]]
   {:db (create db guid size)}))

(rf/reg-event-fx
 ::open
 [persist]
 (fn [_ [_ file-path]]
   (if system/electron?
     {::window.fx/ipc-invoke {:channel "open-documents"
                              :data file-path
                              :on-resolution ::load-multiple
                              :formatter #(mapv edn/read-string %)}}
     {::app.fx/open {:options file-picker-options
                     :on-resolution ::load
                     :formatter #(-> (edn/read-string %)
                                     (assoc :title (.-name %)
                                            :path (.-path %)))}})))

(rf/reg-event-fx
 ::open-directory
 (fn [_ [_ path]]
   {:ipc-send ["open-directory" path]}))

(rf/reg-event-fx
 ::load
 [(rf/inject-cofx ::app.fx/guid)
  (finalize "Load document")]
 (fn [{:keys [db guid]} [_ document]]
   (if (and document (map? document) (:elements document))
     (let [migrated-document (compatibility/migrate-document document)
           migrated (not= document migrated-document)
           document (assoc migrated-document :id guid)]
       (cond-> {:db (h/load db document)}
         (not migrated)
         (assoc :dispatch [::saved document])))
     {:db (notification.h/add db (notification.v/generic-error {:title (str "Error while loading " (:title document))
                                                                :message "File appears to be unsupported or corrupted."}))})))

(rf/reg-event-fx
 ::load-multiple
 (fn [_ [_ documents]]
   {:dispatch-n (mapv #(vector ::load %) documents)}))

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} [_]]
   (let [document (h/persisted-format db)]
     (if system/electron?
       {::window.fx/ipc-invoke {:channel "save-document"
                                :data (pr-str document)
                                :on-resolution ::saved
                                :formatter edn/read-string}}
       {::app.fx/save {:data (h/save-format document)
                       :options file-picker-options
                       :formatter (fn [file] {:id (:id document)
                                              :title (.-name file)})
                       :on-resolution ::saved}}))))

(rf/reg-event-fx
 ::download
 (fn [{:keys [db]} [_]]
   (let [document (h/persisted-format db)]
     {::app.fx/download {:data (h/save-format document)
                         :title (str "document." config/ext)}})))

(rf/reg-event-fx
 ::save-and-close
 (fn [{:keys [db]} [_ id]]
   (let [document (h/persisted-format db id)]
     (if system/electron?
       {::window.fx/ipc-invoke {:channel "save-document"
                                :data (pr-str document)
                                :on-resolution ::close-saved
                                :formatter edn/read-string}}
       {::app.fx/save {:data (h/save-format document)
                       :options file-picker-options
                       :formatter (fn [file] {:id id
                                              :title (.-name file)})
                       :on-resolution ::close-saved}}))))

(rf/reg-event-fx
 ::save-as
 (fn [{:keys [db]} [_]]
   (let [document (h/persisted-format db)]
     (if system/electron?
       {::window.fx/ipc-invoke {:channel "save-document-as"
                                :data (pr-str document)
                                :on-resolution ::saved
                                :formatter edn/read-string}}
       {::app.fx/save {:data (h/save-format document)
                       :options file-picker-options
                       :formatter (fn [file] {:id (:id document)
                                              :title (.-name file)})
                       :on-resolution ::saved}}))))

(rf/reg-event-db
 ::saved
 [persist]
 (fn [db [_ document-info]]
   (if document-info
     (let [{:keys [path id]} document-info
           position (get-in db [:documents id :history :position])]
       (cond-> db
         :always
         (update-in [:documents id] merge (assoc document-info :save position))

         path
         (h/add-recent path))) db)))

(rf/reg-event-fx
 ::close-saved
 (fn [_ [_ document-info]]
   {:dispatch-n [[::saved document-info]
                 [::close (:id document-info) false]]}))

(rf/reg-event-db
 ::clear-recent
 [persist]
 (fn [db [_]]
   (assoc db :recent [])))

(rf/reg-event-db
 ::set-active
 [persist]
 (fn [db [_ id]]
   (-> db
       (h/set-active id)
       (h/center))))
