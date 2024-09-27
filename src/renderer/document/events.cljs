(ns renderer.document.events
  (:require
   [cljs.reader :as edn]
   [platform :as platform]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx :refer [persist]]
   [renderer.app.events :as-alias app.e]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.dialog.handlers :as dialog.h]
   [renderer.dialog.views :as dialog.v]
   [renderer.document.db :as db]
   [renderer.document.effects :as fx]
   [renderer.document.handlers :as h]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h :refer [finalize]]
   [renderer.utils.compatibility :as compatibility]
   [renderer.utils.dom :as dom]
   [renderer.utils.vec :as vec]
   [renderer.window.effects :as-alias window.fx]))

(def active-path
  (let [db-store-key :re-frame-path/db-store]
    (rf/->interceptor
     :id ::active-path
     :before (fn [context]
               (let [original-db (rf/get-coeffect context :db)]
                 (-> context
                     (update db-store-key conj original-db)
                     (rf/assoc-coeffect :db (get-in original-db [:documents (:active-document original-db)])))))
     :after (fn [context]
              (let [db-store (db-store-key context)
                    original-db (peek db-store)
                    new-db-store (pop db-store)
                    context' (-> (assoc context db-store-key new-db-store)
                                 (rf/assoc-coeffect :db original-db))
                    db (rf/get-effect context :db ::not-found)]
                (cond-> context'
                  (not= db ::not-found)
                  (rf/assoc-effect :db (assoc-in original-db [:documents (:active-document original-db)] db))))))))

(rf/reg-event-db
 ::center
 [persist]
 h/center)

(rf/reg-event-db
 ::set-hovered-id
 [active-path]
 (fn [db [_ id]]
   (assoc db :hovered-ids #{id})))

(rf/reg-event-db
 ::clear-hovered
 [active-path]
 (fn [db [_]]
   (assoc db :hovered-ids #{})))

(rf/reg-event-db
 ::collapse-el
 [persist
  active-path]
 (fn [db [_ id]]
   (update db :collapsed-ids conj id)))

(rf/reg-event-db
 ::expand-el
 [persist
  active-path]
 (fn [db [_ id]]
   (update db :collapsed-ids disj id)))

(rf/reg-event-db
 ::toggle-filter
 [persist
  active-path]
 (fn [db [_ id]]
   (if (= (:filter db) id)
     (dissoc db :filter)
     (assoc db :filter id))))

(rf/reg-event-db
 ::swap-colors
 [persist
  active-path]
 (fn [db [_]]
   (assoc db
          :fill (:stroke db)
          :stroke (:fill db))))

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
                           :content [dialog.v/save (get-in db [:documents id])]
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
   (let [to-be-closed (disj (sorted-set (:document-tabs db)) id)]
     {:fx (mapv (fn [id] [:dispatch [::close id true]]) to-be-closed)})))

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
   (if platform/electron?
     {::window.fx/ipc-invoke {:channel "open-documents"
                              :data file-path
                              :on-resolution ::load-multiple
                              :formatter #(mapv edn/read-string %)}}
     {::fx/open nil})))

(rf/reg-event-fx
 ::open-directory
 (fn [_ [_ path]]
   {:ipc-send ["open-directory" path]}))

(rf/reg-event-fx
 ::load
 [(rf/inject-cofx ::app.fx/guid)
  (finalize "Load document")]
 (fn [{:keys [db guid]} [_ document]]
   (let [migrated-document (compatibility/migrate-document document)
         migrated (not= document migrated-document)
         document (assoc migrated-document :id guid)]
     {:db (h/load db document)
      :dispatch (when (not migrated) [::saved document])})))

(rf/reg-event-fx
 ::load-multiple
 (fn [_ [_ documents]]
   {:dispatch-n (mapv #(vector ::load %) documents)}))

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} [_]]
   (let [document (h/save-format db)]
     (if platform/electron?
       {::window.fx/ipc-invoke {:channel "save-document"
                                :data (pr-str document)
                                :on-resolution ::saved
                                :formatter edn/read-string}}
       ;; The path is not available when we use web APIs for security reasons,
       ;; so we always dispatch save-as.
       {::fx/save-as document}))))

(rf/reg-event-fx
 ::download
 (fn [{:keys [db]} [_]]
   (let [document (-> db h/save-format (dissoc :path :id :title))]
     {::fx/download (pr-str document)})))

(rf/reg-event-fx
 ::save-and-close
 (fn [{:keys [db]} [_ id]]
   (let [document (h/save-format db id)]
     (if platform/electron?
       {::window.fx/ipc-invoke {:channel "save-document"
                                :data (pr-str document)
                                :on-resolution ::close-saved
                                :formatter edn/read-string}}
       {::fx/save-as document}))))

(rf/reg-event-fx
 ::save-as
 (fn [{:keys [db]} [_]]
   (let [document (h/save-format db)]
     (if platform/electron?
       {::window.fx/ipc-invoke {:channel "save-document-as"
                                :data (pr-str document)
                                :on-resolution ::saved
                                :formatter edn/read-string}}
       {::fx/save-as document}))))

(rf/reg-event-db
 ::saved
 [persist]
 (fn [db [_ {:keys [path id] :as document-info}]]
   (let [position (get-in db [:documents id :history :position])]
     (-> db
         (update-in [:documents id] merge (assoc document-info :save position))
         (h/add-recent path)))))

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
