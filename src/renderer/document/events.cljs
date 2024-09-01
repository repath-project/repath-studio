(ns renderer.document.events
  (:require
   [cljs.reader :as edn]
   [platform :as platform]
   [re-frame.core :as rf]
   [re-frame.interceptor :refer [->interceptor get-effect get-coeffect assoc-coeffect assoc-effect]]
   [renderer.app.effects :as-alias app.fx]
   [renderer.app.events :as-alias app.e :refer [persist]]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.dialog.handlers :as dialog.h]
   [renderer.dialog.views :as dialog.v]
   [renderer.document.db :as db]
   [renderer.document.effects :as fx]
   [renderer.document.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.utils.vec :as vec]
   [renderer.window.effects :as-alias window.fx]))

(def active-path
  (let [db-store-key :re-frame-path/db-store]
    (->interceptor
     :id ::active-path
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

(def focus-canvas
  (rf/->interceptor
   :id ::focus-canvas
   :after (fn [context]
            (assoc-effect context :fx [[:dispatch-later {:ms 100 :dispatch [::app.e/focus nil]}]]))))

(rf/reg-event-db
 ::center
 persist
 (fn [db [_]]
   (h/center db)))

(rf/reg-event-db
 ::set-hovered-ids
 active-path
 (fn [db [_ ids]]
   (assoc db :hovered-ids (->> ids (remove nil?) (set)))))

(rf/reg-event-db
 ::collapse-el
 [persist active-path]
 (fn [db [_ id]]
   (update db :collapsed-ids conj id)))

(rf/reg-event-db
 ::expand-el
 [persist active-path]
 (fn [db [_ id]]
   (update db :collapsed-ids disj id)))

(rf/reg-event-db
 ::toggle-filter
 [persist active-path]
 (fn [db [_ id]]
   (if (= (:filter db) id)
     (dissoc db :filter)
     (assoc db :filter id))))

(rf/reg-event-db
 ::swap-colors
 [persist active-path]
 (fn [db [_]]
   (assoc db
          :fill (:stroke db)
          :stroke (:fill db))))

(rf/reg-event-fx
 ::set-fill
 [(rf/inject-cofx ::app.fx/now)
  (rf/inject-cofx ::app.fx/guid)
  persist]
 (fn [{:keys [db now guid]} [_ color]]
   {:db (-> db
            (h/set-global-attr :fill color)
            (history.h/finalize now guid guid "Set fill"))}))

(rf/reg-event-fx
 ::set-stroke
 [(rf/inject-cofx ::app.fx/now)
  (rf/inject-cofx ::app.fx/guid)
  persist]
 (fn [{:keys [db now guid]} [_ color]]
   {:db (-> db
            (h/set-global-attr :stroke color)
            (history.h/finalize now guid guid "Set stroke"))}))

(rf/reg-event-db
 ::close
 persist
 (fn [db [_ id confirm?]]
   (if (or (h/saved? db id) (not confirm?))
     (h/close db id)
     (-> db
         (h/set-active id)
         (dialog.h/create {:title "Do you want to save your changes?"
                           :close-button? true
                           :content [dialog.v/save (get-in db [:documents id])]
                           :attrs {:onOpenAutoFocus #(.preventDefault %)}})))))
(rf/reg-event-fx
 ::close-active
 persist
 (fn [{:keys [db]} [_]]
   {:dispatch [::close (:active-document db) true]}))

(rf/reg-event-db
 ::close-all-saved
 persist
 (fn [db [_]]
   (let [saved (filter #(h/saved? db %) (:document-tabs db))]
     (reduce h/close db saved))))

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
 persist
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
 persist
 (fn [db [_ dragged-key swapped-key]]
   (let [document-tabs (:document-tabs db)
         dragged-index (.indexOf document-tabs dragged-key)
         swapped-index (.indexOf document-tabs swapped-key)]
     (assoc db :document-tabs (vec/swap document-tabs dragged-index swapped-index)))))

(rf/reg-event-fx
 ::new
 [(rf/inject-cofx ::app.fx/now)
  (rf/inject-cofx ::app.fx/guid)
  persist
  focus-canvas]
 (fn [{:keys [db now guid]} [_]]
   {:db (-> db
            (h/create)
            (h/create-tab db/default guid)
            (history.h/finalize now guid "Create document"))}))

(rf/reg-event-fx
 ::init
 [(rf/inject-cofx ::app.fx/now)
  (rf/inject-cofx ::app.fx/guid)
  persist
  focus-canvas]
 (fn [{:keys [db now guid]} [_]]
   {:db (cond-> db
          (not (:active-document db))
          (-> (h/create)
              (h/create-tab db/default guid)
              (history.h/finalize now guid "Init document")))}))

(rf/reg-event-fx
 ::new-from-template
 [(rf/inject-cofx ::app.fx/now)
  (rf/inject-cofx ::app.fx/guid)
  persist
  focus-canvas]
 (fn [{:keys [db now guid]} [_ size]]
   {:db (-> db
            (h/create size)
            (h/create-tab db/default guid)
            (history.h/finalize now guid "Create document from template"))}))

(rf/reg-event-fx
 ::open
 persist
 (fn [_ [_ file-path]]
   (if platform/electron?
     {::window.fx/ipc-invoke {:channel "open-documents"
                              :data file-path
                              :on-resolution ::load
                              :formatter #(mapv edn/read-string %)}}
     {::fx/open nil})))

(rf/reg-event-fx
 ::open-directory
 (fn [_ [_ path]]
   {:ipc-send ["open-directory" path]}))

(rf/reg-event-fx
 ::load
 [(rf/inject-cofx ::app.fx/now)
  (rf/inject-cofx ::app.fx/guid)
  persist
  focus-canvas]
 (fn [{:keys [db now guid]} [_ documents]]
   {:db (->> documents
             (reduce #(h/load %1 %2 now guid) db)
             (h/center))}))

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
   (let [document (h/save-format db)]
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
 persist
 (fn [db [_ {:keys [path id] :as document-info}]]
   ;; Update the path, the title and the saved position of the document.
   ;; Any other changes that could happen while saving should be preserved.
   (-> db
       (update-in [:documents id] merge document-info)
       (h/add-recent path))))

(rf/reg-event-fx
 ::close-saved
 (fn [_ [_ document-info]]
   {:dispatch-n [[::saved document-info]
                 [::close (:id document-info) false]]}))

(rf/reg-event-db
 ::clear-recent
 persist
 (fn [db [_]]
   (assoc db :recent [])))

(rf/reg-event-db
 ::set-active
 persist
 (fn [db [_ id]]
   (-> db
       (h/set-active id)
       (h/center))))
