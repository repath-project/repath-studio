(ns renderer.document.events
  (:require
   [cljs.reader :as cljs.reader]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.events :as-alias app.events :refer [persist]]
   [renderer.dialog.handlers :as dialog.handlers]
   [renderer.dialog.views :as dialog.views]
   [renderer.document.handlers :as document.handlers]
   [renderer.effects :as-alias effects]
   [renderer.element.db :as element.db]
   [renderer.element.effects :as-alias element.effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.events :as-alias events]
   [renderer.history.handlers :as history.handlers]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.compatibility :as utils.compatibility]
   [renderer.utils.element :as utils.element]
   [renderer.utils.i18n :refer [tr]]
   [renderer.utils.vec :as utils.vec]))

(def file-picker-options
  (let [ext (str "." config/ext)]
    {:startIn config/default-path
     :id "file-picker"
     :types [{:accept {config/mime-type [ext]}}]}))

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
   (let [fill (document.handlers/attr db :fill)
         stroke (document.handlers/attr db :stroke)]
     (-> db
         (document.handlers/assoc-attr :fill stroke)
         (document.handlers/assoc-attr :stroke fill)))))

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

(rf/reg-event-fx
 ::close
 [persist]
 (fn [{:keys [db]} [_ id confirm?]]
   {:db (if (or (document.handlers/saved? db id)
                (not confirm?))
          (document.handlers/close db id)
          (-> db
              (document.handlers/set-active id)
              (dialog.handlers/create
               {:title (tr db [::save-changes
                               "Do you want to save your changes?"])
                :has-close-button true
                :content [dialog.views/save (get-in db [:documents id])]
                :attrs {:onOpenAutoFocus #(.preventDefault %)}})))
    ::app.effects/local-store-keys {:on-success [::clear-stale-keys]}}))

(rf/reg-event-fx
 ::close-active
 (fn [{:keys [db]} [_]]
   {:dispatch [::close (:active-document db) true]}))

(rf/reg-event-db
 ::close-saved
 [persist]
 (fn [db [_]]
   (->> (document.handlers/saved-ids db)
        (reduce document.handlers/close db))))

(rf/reg-event-fx
 ::close-others
 (fn [{:keys [db]} [_ id]]
   (let [to-be-closed (-> (apply sorted-set (:document-tabs db))
                          (disj id))]
     {:dispatch-n (->> to-be-closed
                       (mapv (fn [id] [::close id true])))})))

(rf/reg-event-fx
 ::close-all
 (fn [{:keys [db]} [_]]
   (let [document-tabs (:document-tabs db)]
     {:db (document.handlers/set-active db (last document-tabs))
      :fx (->> document-tabs
               (mapv (fn [id] [:dispatch [::close id true]])))})))

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
       (assoc :document-tabs (utils.vec/swap document-tabs
                                             dragged-i
                                             swapped-i))))))

(rf/reg-event-fx
 ::new
 [(rf/inject-cofx ::effects/guid)]
 (fn [{:keys [db guid]} [_]]
   {:db (-> (document.handlers/create db guid)
            (history.handlers/finalize [::create-doc "Create document"]))
    ::effects/focus nil}))

(rf/reg-event-fx
 ::new-from-template
 [(rf/inject-cofx ::effects/guid)]
 (fn [{:keys [db guid]} [_ size]]
   {:db (-> (document.handlers/create db guid size)
            (history.handlers/finalize [::create-doc-from-template
                                        "Create document from template"]))}))

(defn string->edn
  [s]
  (try (cljs.reader/read-string s)
       (catch :default _err nil)))

(rf/reg-event-fx
 ::open
 (fn [{:keys [db]} [_]]
   (if (= (:platform db) "web")
     {::effects/file-open
      {:options (assoc file-picker-options :multiple true)
       :on-success [::file-read nil]
       :on-error [::app.events/toast-error]}}
     {::effects/ipc-invoke
      {:channel "open-documents"
       :on-success [::load-multiple]
       :on-error [::app.events/toast-error]
       :formatter #(mapv string->edn %)}})))

(rf/reg-event-fx
 ::open-recent
 (fn [{:keys [db]} [_ {:keys [id path]}]]
   (if (document.handlers/open? db id)
     {:db (document.handlers/set-active db id)}
     (if (= (:platform db) "web")
       {::app.effects/get-local-store
        {:store-key (str id)
         :formatter (fn [file-handle]
                      {:on-success [::file-read id]
                       :on-error [::recent-error id]
                       :file-handle file-handle})
         :on-success [::events/file-open]
         :on-error [::recent-error id]}}
       {::effects/ipc-invoke
        {:channel "open-documents"
         :data path
         :on-success [::load-multiple]
         :on-error [::recent-error id]
         :formatter #(mapv string->edn %)}}))))

(rf/reg-event-fx
 ::recent-error
 [persist]
 (fn [{:keys [db]} [_ id error]]
   {:db (document.handlers/remove-recent db id)
    :fx [[::app.effects/remove-local-store
          {:store-key (str id)
           :on-error [::app.events/toast-error]}]
         (when error
           [:dispatch [::app.events/toast-error error]])]}))

(rf/reg-event-fx
 ::file-read
 (fn [_ [_ id ^js/FileSystemFileHandle file-handle ^js/File file]]
   {::effects/file-read-as
    [file :text {"load" {:formatter #(when-let [data (string->edn %)]
                                       (when (map? data)
                                         (cond-> data
                                           id
                                           (assoc :id id)

                                           (.-name file)
                                           (assoc :title (.-name file))

                                           (.-path file)
                                           (assoc :path (.-path file))

                                           file-handle
                                           (assoc :file-handle file-handle))))
                         :on-fire [::load]}
                 "error" {:on-fire [::app.events/toast-error]}}]}))

(rf/reg-event-fx
 ::open-directory
 (fn [_ [_ path]]
   {::effects/ipc-send ["open-directory" path]}))

(rf/reg-event-fx
 ::load
 [(rf/inject-cofx ::effects/guid)]
 (fn [{:keys [db guid]} [_ document]]
   (if (map? document)
     (let [migrated-document (utils.compatibility/migrate-document document)
           is-migrated (not= document migrated-document)
           document (merge {:id guid} migrated-document)
           save-info (document.handlers/save-info document)
           {:keys [id file-handle]} document]
       {:db (cond-> db
              :always
              (-> (document.handlers/load (dissoc document :file-handle))
                  (history.handlers/finalize [::load-doc "Load document"]))

              (not is-migrated)
              (document.handlers/update-save-info save-info))
        :fx [(when file-handle
               [::app.effects/set-local-store
                {:data file-handle
                 :store-key (str id)
                 :on-error [::app.events/toast-error]}])
             [::effects/focus nil]]})
     {::app.effects/toast
      [:error
       (tr db [::error-loading "Error while loading %1"] [(:title document)])
       {:description (tr db [::unsupported-or-corrupted
                             "File appears to be unsupported or corrupted."])}]})))

(rf/reg-event-fx
 ::load-multiple
 (fn [_ [_ documents]]
   {:dispatch-n (mapv #(vector ::load %) documents)}))

(defn- file-save-options
  [document on-success on-error]
  {:data (document.handlers/->save-format document)
   :options file-picker-options
   :formatter (fn [file-handle]
                {:id (:id document)
                 :title (.-name file-handle)
                 :file-handle file-handle})
   :on-success on-success
   :on-error on-error})

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} [_ {:keys [close id]}]]
   (let [id (or id (:active-document db))
         document (document.handlers/persisted-format db id)
         on-success [::saved close]
         on-error [::app.events/toast-error]]
     (if (= (:platform db) "web")
       {::app.effects/get-local-store
        {:store-key (str id)
         :formatter #(-> document
                         (file-save-options on-success on-error)
                         (assoc :file-handle %))
         :on-success [::events/file-save]
         :on-error on-error}}
       {::effects/ipc-invoke
        {:channel "save-document"
         :data (pr-str document)
         :on-success [::saved close]
         :on-error on-error
         :formatter string->edn}}))))

(rf/reg-event-fx
 ::save-as
 (fn [{:keys [db]} [_ _]]
   (let [id (:active-document db)
         document (document.handlers/persisted-format db id)
         on-success [::saved false]
         on-error [::app.events/toast-error]]
     (if (= (:platform db) "web")
       {::effects/file-save (file-save-options document on-success on-error)}
       {::effects/ipc-invoke
        {:channel "save-document-as"
         :data (pr-str document)
         :on-success on-success
         :on-error on-error
         :formatter string->edn}}))))

(rf/reg-event-fx
 ::download
 (fn [{:keys [db]} [_]]
   (when-let [data (some->> (:active-document db)
                            (document.handlers/persisted-format db)
                            (document.handlers/->save-format))]
     {::effects/download {:data data
                          :title (str "document." config/ext)}})))

(rf/reg-event-fx
 ::saved
 [persist]
 (fn [{:keys [db]} [_ close info]]
   (let [{:keys [id file-handle]} info
         save-info (document.handlers/save-info info)]
     {:db (cond-> db
            :always
            (-> (document.handlers/update-save-info save-info)
                (document.handlers/add-recent save-info))

            close
            (document.handlers/close id))
      :fx [(when file-handle
             [::app.effects/set-local-store
              {:data file-handle
               :store-key (str id)
               :on-error [::app.events/toast-error]}])]})))

(rf/reg-event-fx
 ::clear-recent
 [persist]
 (fn [{:keys [db]} [_]]
   {:db (assoc db :recent [])
    ::app.effects/local-store-keys {:on-success [::clear-stale-keys]}}))

(rf/reg-event-fx
 ::clear-stale-keys
 (fn [{:keys [db]} [_ storage-keys]]
   {:fx (->> storage-keys
             (remove #(= % config/app-name))
             (map uuid)
             (remove #(or (document.handlers/open? db %)
                          (document.handlers/recent? db %)))
             (map #(vector ::app.effects/remove-local-store
                           {:store-key (str %)}))
             (into []))}))

(rf/reg-event-db
 ::set-active
 [persist]
 (fn [db [_ id]]
   (-> db
       (document.handlers/set-active id)
       (document.handlers/center))))

(rf/reg-event-db
 ::center
 [persist]
 (fn [db [_]]
   (document.handlers/center db)))

(rf/reg-event-fx
 ::export
 (fn [{:keys [db]} [_ mime-type]]
   (let [els (element.handlers/root-children db)
         svg (utils.element/->svg els)]
     (case mime-type
       "image/svg+xml"
       {::effects/file-save
        {:data svg
         :on-error [::app.events/toast-error]
         :options {:id "export"
                   :startIn "pictures"
                   :types [{:accept {"image/svg+xml" [".svg"]}}]}}}

       {::element.effects/export-image
        {:data svg
         :mime-type mime-type
         :size (utils.bounds/->dimensions (utils.element/united-bbox els))
         :on-success [::save-rasterized-image]
         :on-error [::app.events/toast-error]}}))))

(rf/reg-event-fx
 ::save-rasterized-image
 (fn [_ [_ data mime-type]]
   (let [extensions (get element.db/image-mime-types mime-type)]
     {::effects/file-save
      {:data data
       :on-error [::app.events/toast-error]
       :options {:id "export"
                 :startIn "pictures"
                 :types [{:accept {mime-type (or extensions
                                                 [])}}]}}})))

(rf/reg-event-fx
 ::print
 (fn [{:keys [db]} _]
   (let [els (element.handlers/root-children db)
         svg (utils.element/->svg els)]
     (if (:platmform db)
       {::effects/ipc-invoke
        {:channel "print"
         :data svg
         :on-success [::app.events/toast :success]
         :on-error [::app.events/toast-error]}}
       {::effects/print svg}))))
