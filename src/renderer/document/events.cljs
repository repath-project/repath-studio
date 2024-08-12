(ns renderer.document.events
  (:require
   [cljs.reader :as edn]
   [platform :as platform]
   [re-frame.core :as rf]
   [re-frame.interceptor :refer [->interceptor get-effect get-coeffect assoc-coeffect assoc-effect]]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.document.effects :as fx]
   [renderer.document.handlers :as h]
   [renderer.frame.events :as-alias frame.e]
   [renderer.utils.local-storage :as local-storage]
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
   (assoc db :hovered-keys (->> ks (remove nil?) (set)))))

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
 (fn [db [_ fill]]
   (h/set-global-attr db :fill fill)))

(rf/reg-event-db
 ::set-stroke
 (fn [db [_ stroke]]
   (h/set-global-attr db :stroke stroke)))

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
   {:db (h/new db)
    :fx [[:dispatch [::frame.e/center]]
         [:focus nil]]}))

(rf/reg-event-fx
 ::open
 (fn [_ [_ file-path]]
   (if platform/electron?
     {:ipc-invoke ["open-documents"
                   file-path
                   #(rf/dispatch [::load (mapv edn/read-string %)])]}
     {::fx/open nil})))

(rf/reg-event-fx
 ::open-directory
 (fn [_ [_ path]]
   {:ipc-send ["open-directory" path]}))

(rf/reg-event-fx
 ::load
 local-storage/persist
 (fn [{:keys [db]} [_ documents]]
   {:db (reduce h/load db documents)
    :fx [[:dispatch [::frame.e/center]]
         [:focus nil]]}))

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} [_]]
   (let [document (h/save-format db)]
     (if platform/electron?
       {:ipc-invoke ["save-document"
                     (pr-str document)
                     #(rf/dispatch [::saved (edn/read-string %)])]}
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
 (fn [{:keys [db]} [_ k]]
   (let [document (h/save-format db k)]
     (if platform/electron?
       {:ipc-invoke ["save-document"
                     (pr-str document)
                     #(do (rf/dispatch [::saved (edn/read-string %)])
                          (rf/dispatch [::close k false]))]}
       {::fx/save-as document}))))

(rf/reg-event-fx
 ::save-as
 (fn [{:keys [db]} [_]]
   (let [document (h/save-format db)]
     (if platform/electron?
       {:ipc-invoke ["save-document-as"
                     (pr-str document)
                     #(rf/dispatch [::saved (edn/read-string %)])]}
       {::fx/save-as document}))))

(rf/reg-event-db
 ::saved
 (fn [db [_ {:keys [path] :as document-info}]]
   ;; Update the path, the title and the saved position of the document.
   ;; Any other changes that could happen while saving should be preserved.
   (-> db
       (update-in [:documents (h/search-by-path db path)] merge document-info)
       (h/add-recent path))))

(rf/reg-event-db
 ::clear-recent
 local-storage/persist
 (fn [db [_]]
   (assoc db :recent [])))

(rf/reg-event-db
 ::set-active
 (fn [db [_ k]]
   (h/set-active db k)))
