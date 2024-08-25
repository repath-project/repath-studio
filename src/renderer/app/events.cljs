(ns renderer.app.events
  (:require
   [akiroz.re-frame.storage :as rf.storage]
   [config :as config]
   [malli.core :as m]
   [malli.transform :as mt]
   [platform :as platform]
   [re-frame.core :as rf]
   [renderer.app.db :as db]
   [renderer.app.effects :as fx]
   [renderer.app.handlers :as h]
   [renderer.frame.handlers :as frame.h]
   [renderer.notification.events :as-alias notification.e]
   [renderer.tool.base :as tool]
   [renderer.utils.local-storage :as local-storage]
   [renderer.utils.pointer :as pointer]
   [renderer.window.effects :as-alias window.fx]))

(def custom-fx
  (rf/->interceptor
   :id ::custom-fx
   :after (fn [context]
            (let [db (rf/get-effect context :db ::not-found)]
              (cond-> context
                (not= db ::not-found)
                (-> (rf/assoc-effect :fx (apply conj (or (:fx (rf/get-effect context)) []) (:fx db)))
                    (rf/assoc-effect :db (assoc db :fx []))))))))

(rf/reg-global-interceptor custom-fx)

(rf.storage/reg-co-fx! :repath-studio {:cofx :store})

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   (-> db/app
       (m/decode {} mt/default-value-transformer)
       (assoc :version config/version))))

(defn compatible-versions?
  [v1 v2]
  (let [reg #"(\d+\.)?(\d+\.)"]
    (when (and (string? v1) (string? v2))
      (= (re-find reg v1) (re-find reg v2)))))

(rf/reg-event-fx
 ::load-local-db
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [compatible? (compatible-versions? (:version db) (:version store))]
     {:db (cond-> db compatible? (merge db store))
      ;; TODO: Introduce migrations to handle this gracefully.
      :fx [(when-not compatible? [::fx/local-storage-clear nil])]})))

(rf/reg-event-db
 ::set-system-fonts
 (fn [db [_ fonts]]
   (assoc db :system-fonts fonts)))

(rf/reg-event-db
 ::set-webref-css
 (fn [db [_ webref-css]]
   (assoc db :webref-css webref-css)))

(rf/reg-event-db
 ::set-mdn
 (fn [db [_ mdn]]
   (assoc db :mdn mdn)))

(rf/reg-event-fx
 ::set-tool
 (fn [{:keys [db]} [_ tool]]
   {:db (h/set-tool db tool)
    ::fx/focus nil}))

#_(rf/reg-event-db
   :set-lang
   (fn [db [_ lang]]
     (assoc db :lang lang)))

(rf/reg-event-db
 ::set-repl-mode
 (fn [db [_ mode]]
   (assoc db :repl-mode mode)))

(rf/reg-event-db
 ::toggle-debug-info
 (fn [db [_]]
   (update db :debug-info? not)))

(rf/reg-event-db
 ::set-backdrop
 (fn [db [_ visible?]]
   (assoc db :backdrop? visible?)))

(rf/reg-event-db
 ::toggle-rulers
 local-storage/persist
 (fn [db [_]]
   (update db :rulers-visible? not)))

#_(rf/reg-event-db
   ::toggle-rulers-locked
   (fn [db [_]]
     (update db :rulers-locked? not)))

(rf/reg-event-db
 ::toggle-grid
 local-storage/persist
 (fn [db [_]]
   (update db :grid-visible? not)))

(rf/reg-event-db
 ::toggle-panel
 [local-storage/persist
  (rf/path :panels)]
 (fn [db [_ k]]
   (update-in db [k :visible?] not)))

(rf/reg-event-fx
 ::pointer-event
 (fn [{:keys [db]} [_ {:keys [button buttons modifiers data-transfer pointer-pos delta element] :as e}]]
   (let [{:keys [pointer-offset tool dom-rect drag? primary-tool]} db
         adjusted-pointer-pos (frame.h/adjust-pointer-pos db pointer-pos)]
     {:db (case (:type e)
            :pointermove
            (if (= buttons :right)
              db
              (-> (if pointer-offset
                    (if (pointer/significant-drag? pointer-pos pointer-offset)
                      (cond-> db
                        (not= tool :pan)
                        (frame.h/pan-out-of-canvas dom-rect
                                                   pointer-pos
                                                   pointer-offset)

                        (not drag?)
                        (-> (tool/drag-start e)
                            (assoc :drag? true))

                        :always
                        (tool/drag e))
                      db)
                    (tool/pointer-move db e))
                  (assoc :pointer-pos pointer-pos
                         :adjusted-pointer-pos adjusted-pointer-pos)))

            :pointerdown
            (cond-> db
              (= button :middle)
              (-> (assoc :primary-tool tool)
                  (h/set-tool :pan))

              (and (= button :right) (not= (:id element) :bounding-box))
              (tool/pointer-up e)

              :always
              (-> (tool/pointer-down e)
                  (assoc :pointer-offset pointer-pos
                         :adjusted-pointer-offset adjusted-pointer-pos)))

            :pointerup
            (cond-> (if drag?
                      (tool/drag-end db e)
                      (cond-> db (not= button :right) (tool/pointer-up e)))
              (and primary-tool (= button :middle))
              (-> (h/set-tool primary-tool)
                  (dissoc :primary-tool))

              :always
              (-> (dissoc :pointer-offset :drag?)
                  (update :snap dissoc :nearest-neighbor)))

            :dblclick
            (tool/double-click db e)

            :wheel
            (if (some modifiers [:ctrl :alt])
              (let [delta-y (second delta)
                    factor (Math/pow (inc (/ (- 1 (:zoom-sensitivity db)) 100))
                                     (- delta-y))]
                (frame.h/zoom-in-pointer-position db factor))
              (frame.h/pan-by db delta))

            db)
      :fx [(case (:type e)
             :drop
             [::fx/data-transfer [adjusted-pointer-pos data-transfer]]

             :pointerdown
             [::fx/set-pointer-capture [(:target e) (:pointer-id e)]]

             nil)]})))

(rf/reg-event-db
 :keyboard-event
 (fn [{:keys [tool] :as db} [_ {:keys [type code] :as e}]]
   (case type
     :keydown
     (cond-> db
       (and (= code "Space")
            (not= tool :pan))
       (-> (assoc :primary-tool tool)
           (h/set-tool :pan))

       :always
       (tool/key-down e))

     :keyup
     (cond-> db
       (and (= code "Space")
            (:primary-tool db))
       (-> (h/set-tool (:primary-tool db))
           (dissoc :primary-tool))

       :always
       (tool/key-up e))

     db)))

(rf/reg-event-fx
 ::focus
 (fn [_ [_ id]]
   {::fx/focus id}))

(rf/reg-event-fx
 ::load-system-fonts
 (fn [_ [_ file-path]]
   (if platform/electron?
     {::window.fx/ipc-invoke {:channel "load-system-fonts"
                              :data file-path
                              :on-resolution ::set-system-fonts
                              :formatter #(js->clj % :keywordize-keys true)}}
     {::fx/load-system-fonts nil})))

(rf/reg-event-fx
 ::load-webref
 (fn [_ [_ file-path]]
   {::window.fx/ipc-invoke {:channel "load-webref"
                            :data file-path
                            :on-resolution ::set-webref-css
                            :formatter #(js->clj % :keywordize-keys true)}}))
