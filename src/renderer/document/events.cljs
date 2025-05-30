(ns renderer.document.events
  (:require
   [cljs.reader :as cljs.reader]
   [config :as config]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.events :refer [persist]]
   [renderer.dialog.handlers :as dialog.handlers]
   [renderer.dialog.views :as dialog.views]
   [renderer.document.db :as document.db]
   [renderer.document.effects :as document.effects]
   [renderer.document.handlers :as document.handlers]
   [renderer.effects :as-alias effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.notification.events :as-alias notification.events]
   [renderer.notification.handlers :as notification.handlers]
   [renderer.notification.views :as notification.views]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.utils.compatibility :as utils.compatibility]
   [renderer.utils.element :as utils.element]
   [renderer.utils.math :refer [Vec2]]
   [renderer.utils.vec :as utils.vec]))

(def file-picker-options
  {:startIn config/default-path
   :types [{:accept {config/mime-type [(str "." config/ext)]}}]})

(rf/reg-event-db
 ::set-hovered-id
 (fn [db [_ id]]
   (document.handlers/set-hovered-ids db #{id})))

(rf/reg-event-db
 ::clear-hovered
 (fn [db [_]]
   (document.handlers/set-hovered-ids db #{})))

(rf/reg-event-db
 ::collapse-el
 [persist]
 (fn [db [_ id]]
   (document.handlers/collapse-el db id)))

(rf/reg-event-db
 ::expand-el
 [persist]
 (fn [db [_ id]]
   (document.handlers/expand-el db id)))

(rf/reg-event-db
 ::toggle-filter
 [persist]
 (fn [db [_ id]]
   (if (= (:filter (document.handlers/active db)) id)
     (update-in db (document.handlers/path db) dissoc :filter)
     (assoc-in db (document.handlers/path db :filter) id))))

(rf/reg-event-db
 ::swap-colors
 [persist]
 (fn [db [_]]
   (-> db
       (document.handlers/assoc-attr :fill (document.handlers/attr db :stroke))
       (document.handlers/assoc-attr :stroke (document.handlers/attr db :fill)))))

(rf/reg-event-db
 ::set-attr
 (fn [db [_ k v]]
   (document.handlers/assoc-attr db k v)))

(rf/reg-event-db
 ::preview-attr
 (fn [db [_ k v]]
   (-> db
       (document.handlers/assoc-attr k v)
       (element.handlers/set-attr k v))))

(rf/reg-event-db
 ::close
 [persist]
 (fn [db [_ id confirm?]]
   (if (or (document.handlers/saved? db id)
           (not confirm?))
     (document.handlers/close db id)
     (-> db
         (document.handlers/set-active id)
         (dialog.handlers/create
          {:title "Do you want to save your changes?"
           :close-button true
           :content (dialog.views/save (get-in db [:documents id]))
           :attrs {:onOpenAutoFocus #(.preventDefault %)}})))))

(rf/reg-event-fx
 ::close-active
 (fn [{:keys [db]} [_]]
   {:dispatch [::close (:active-document db) true]}))

(rf/reg-event-db
 ::close-saved
 [persist]
 (fn [db [_]]
   (reduce document.handlers/close db (document.handlers/saved-ids db))))

(rf/reg-event-fx
 ::close-others
 (fn [{:keys [db]} [_ id]]
   (let [to-be-closed (disj (apply sorted-set (:document-tabs db)) id)]
     {:dispatch-n (mapv (fn [id] [::close id true]) to-be-closed)})))

(rf/reg-event-fx
 ::close-all
 (fn [{:keys [db]} [_]]
   {:db (document.handlers/set-active db (last (:document-tabs db)))
    :fx (mapv (fn [id] [:dispatch [::close id true]]) (:document-tabs db))}))

(rf/reg-event-db
 ::cycle
 [persist]
 (fn [db [_ direction]]
   (let [document-tabs (:document-tabs db)
         index (.indexOf document-tabs (:active-document db))
         scrolled-index (if (pos? direction) (inc index) (dec index))]
     (cond-> db
       (and (nat-int? scrolled-index)
            (< scrolled-index (count document-tabs)))
       (document.handlers/set-active (get document-tabs scrolled-index))))))

(rf/reg-event-db
 ::swap-position
 [persist]
 (fn [db [_ dragged-id swapped-id]]
   (let [document-tabs (:document-tabs db)
         dragged-i (.indexOf document-tabs dragged-id)
         swapped-i (.indexOf document-tabs swapped-id)]
     (cond-> db
       (not (or (= dragged-i -1)
                (= swapped-i -1)))
       (assoc :document-tabs (utils.vec/swap document-tabs dragged-i swapped-i))))))

(m/=> create [:function
              [:-> map? uuid? App]
              [:-> map? uuid? [:maybe Vec2] App]])
(defn create
  ([db guid]
   (create db guid [595 842]))
  ([db guid size]
   (-> (document.handlers/create-tab db (assoc document.db/default :id guid))
       (element.handlers/create-default-canvas size)
       (document.handlers/center))))

(rf/reg-event-fx
 ::new
 [(rf/inject-cofx ::effects/guid)]
 (fn [{:keys [db guid]} [_]]
   {:db (-> (create db guid)
            (history.handlers/finalize "Create document"))
    ::effects/focus nil}))

(rf/reg-event-fx
 ::init
 [(rf/inject-cofx ::effects/guid)]
 (fn [{:keys [db guid]} [_]]
   {:db (if (:active-document db)
          (snap.handlers/rebuild-tree db)
          (-> (create db guid)
              (history.handlers/finalize "Init document")))}))

(rf/reg-event-fx
 ::new-from-template
 [(rf/inject-cofx ::effects/guid)]
 (fn [{:keys [db guid]} [_ size]]
   {:db (-> (create db guid size)
            (history.handlers/finalize "Create document from template"))}))

(rf/reg-event-fx
 ::open
 (fn [{:keys [db]} [_ file-path]]
   (if (= (:platform db) "web")
     {::effects/file-open
      {:options file-picker-options
       :on-success [::file-read]
       :on-error [::notification.events/show-exception]}}
     {::effects/ipc-invoke
      {:channel "open-documents"
       :data file-path
       :on-success [::load-multiple]
       :on-error [::notification.events/show-exception]
       :formatter #(mapv cljs.reader/read-string %)}})))

(rf/reg-event-fx
 ::file-read
 (fn [_ [_ file]]
   {::app.effects/file-read-as
    [file :text {"load" {:formatter #(-> (cljs.reader/read-string %)
                                         (assoc :title (.-name file)
                                                :path (.-path file)))
                         :on-fire [::load]}
                 "error" {:on-fire [::notification.events/show-exception]}}]}))

(rf/reg-event-fx
 ::open-directory
 (fn [_ [_ path]]
   {:ipc-send ["open-directory" path]}))

(rf/reg-event-fx
 ::load
 [(rf/inject-cofx ::effects/guid)]
 (fn [{:keys [db guid]} [_ document]]
   (if (and document (map? document) (:elements document))
     (let [migrated-document (utils.compatibility/migrate-document document)
           migrated (not= document migrated-document)
           document (assoc migrated-document :id guid)]
       (cond-> {:db (-> (document.handlers/load db document)
                        (history.handlers/finalize "Load document"))
                ::effects/focus nil}
         (not migrated)
         (assoc :dispatch [::saved document])))
     {:db (->> (notification.views/generic-error
                {:title (str "Error while loading " (:title document))
                 :message "File appears to be unsupported or corrupted."})
               (notification.handlers/add db))})))

(rf/reg-event-fx
 ::load-multiple
 (fn [_ [_ documents]]
   {:dispatch-n (mapv #(vector ::load %) documents)}))

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} [_]]
   (let [document (document.handlers/persisted-format db)]
     (if (= (:platform db) "web")
       {::document.effects/file-save
        {:data (document.handlers/save-format document)
         :options file-picker-options
         :formatter (fn [file] {:id (:id document)
                                :title (.-name file)})
         :on-success [::saved]
         :on-error [::notification.events/show-exception]}}
       {::effects/ipc-invoke
        {:channel "save-document"
         :data (pr-str document)
         :on-success [::saved]
         :on-error [::notification.events/show-exception]
         :formatter cljs.reader/read-string}}))))

(rf/reg-event-fx
 ::download
 (fn [{:keys [db]} [_]]
   (let [document (document.handlers/persisted-format db)]
     {::effects/download {:data (document.handlers/save-format document)
                          :title (str "document." config/ext)}})))

(rf/reg-event-fx
 ::save-and-close
 (fn [{:keys [db]} [_ id]]
   (let [document (document.handlers/persisted-format db id)]
     (if (= (:platform db) "web")
       {::document.effects/file-save
        {:data (document.handlers/save-format document)
         :options file-picker-options
         :formatter (fn [file] {:id id
                                :title (.-name file)})
         :on-success [::mark-as-saved-and-close]
         :on-error [::notification.events/show-exception]}}
       {::effects/ipc-invoke
        {:channel "save-document"
         :data (pr-str document)
         :on-success [::mark-as-saved-and-close]
         :on-error [::notification.events/show-exception]
         :formatter cljs.reader/read-string}}))))

(rf/reg-event-fx
 ::save-as
 (fn [{:keys [db]} [_]]
   (let [document (document.handlers/persisted-format db)]
     (if (= (:platform db) "web")
       {::document.effects/file-save
        {:data (document.handlers/save-format document)
         :options file-picker-options
         :formatter (fn [file] {:id (:id document)
                                :title (.-name file)})
         :on-success [::saved]
         :on-error [::notification.events/show-exception]}}
       {::effects/ipc-invoke
        {:channel "save-document-as"
         :data (pr-str document)
         :on-success [::saved]
         :on-error [::notification.events/show-exception]
         :formatter cljs.reader/read-string}}))))

(rf/reg-event-db
 ::saved
 [persist]
 (fn [db [_ document-info]]
   (if document-info
     (let [{:keys [id]} document-info
           position (get-in db [:documents id :history :position])]
       (cond-> db
         :always
         (update-in [:documents id] merge (assoc document-info :save position))

         (:path document-info)
         (document.handlers/add-recent (:path document-info))))
     db)))

(rf/reg-event-fx
 ::mark-as-saved-and-close
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
   (-> (document.handlers/set-active db id)
       (document.handlers/center))))

(rf/reg-event-fx
 ::export-svg
 (fn [{:keys [db]} _]
   (let [els (element.handlers/root-children db)
         svg (utils.element/->svg els)]
     (if (:platmform db)
       {::effects/ipc-invoke
        {:channel "export"
         :data svg
         :on-error [::notification.events/show-exception]}}
       {::document.effects/file-save
        [:data svg
         :on-error [::notification.events/show-exception]
         :options {:startIn "pictures"
                   :types [{:accept {"image/svg+xml" [".svg"]}}]}]}))))

(rf/reg-event-fx
 ::print
 (fn [{:keys [db]} _]
   (let [els (element.handlers/root-children db)
         svg (utils.element/->svg els)]
     (if (:platmform db)
       {::effects/ipc-invoke
        {:channel "print"
         :data svg
         :on-success [::notification.events/add]
         :on-error [::notification.events/show-exception]}}
       {::effects/print svg}))))
