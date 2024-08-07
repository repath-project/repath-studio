(ns renderer.events
  (:require
   [malli.core :as ma]
   [malli.error :as ma.error]
   [re-frame.core :as rf]
   [renderer.db :as db]
   [renderer.effects]
   [renderer.handlers :as h]
   [renderer.frame.handlers :as frame-h]
   [renderer.notification.events :as-alias notification.e]
   [renderer.tool.base :as tool]
   [renderer.utils.local-storage :as local-storage]
   [renderer.utils.pointer :as pointer]))

(def valid? (ma/validator db/app))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec"
  [state event]
  (when-not (valid? state)
    (js/console.error (str "Event: " (first event)))
    (throw (js/Error. (str "Spec check failed: " (-> (ma/explain db/app state)
                                                     ma.error/humanize
                                                     str))))))

(def schema-valdator (rf/after (partial check-and-throw)))

(rf/reg-global-interceptor schema-valdator)

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   db/default))

(rf/reg-event-db
 :load-local-db
 local-storage/persist
 (fn [db _]
   db))

(rf/reg-event-db
 :set-system-fonts
 (fn [db [_ fonts]]
   (assoc db :system-fonts fonts)))

(rf/reg-event-db
 :set-webref-css
 (fn [db [_ webref-css]]
   (assoc db :webref-css webref-css)))

(rf/reg-event-db
 :set-mdn
 (fn [db [_ mdn]]
   (assoc db :mdn mdn)))

(rf/reg-event-fx
 :set-tool
 (fn [{:keys [db]} [_ tool]]
   {:db (h/set-tool db tool)
    :focus nil}))

(rf/reg-event-db
 :clear-restored
 (fn [db [_]]
   (dissoc db :restored?)))

#_(rf/reg-event-db
   :set-lang
   (fn [db [_ lang]]
     (assoc db :lang lang)))

(rf/reg-event-db
 :set-repl-mode
 (fn [db [_ mode]]
   (assoc db :repl-mode mode)))

(rf/reg-event-db
 :toggle-debug-info
 (fn [db [_]]
   (update db :debug-info? not)))

(rf/reg-event-db
 :set-backdrop
 (fn [db [_ backdrop?]]
   (assoc db :backdrop? backdrop?)))

(rf/reg-event-db
 :toggle-rulers
 (fn [db [_]]
   (update db :rulers-visible? not)))

#_(rf/reg-event-db
   :toggle-rulers-locked
   (fn [db [_]]
     (update db :rulers-locked? not)))

(rf/reg-event-db
 :toggle-grid
 (fn [db [_]]
   (update db :grid-visible? not)))

(rf/reg-event-db
 :toggle-panel
 [local-storage/persist
  (rf/path :panel)]
 (fn [db [_ key]]
   (update-in db [key :visible?] not)))

(rf/reg-event-fx
 :pointer-event
 (fn [{:keys [db]} [_ {:keys [button buttons modifiers data-transfer pointer-pos delta element] :as e}]]
   (let [{:keys [pointer-offset tool dom-rect drag? primary-tool]} db
         adjusted-pointer-pos (frame-h/adjusted-pointer-pos db pointer-pos)]
     {:db (case (:type e)
            :pointermove
            (if (= buttons :right)
              db
              (-> (if pointer-offset
                    (if (pointer/significant-drag? pointer-pos pointer-offset)
                      (cond-> db
                        (not= tool :pan)
                        (frame-h/pan-out-of-canvas dom-rect
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

              (and (= button :right) (not= (:key element) :bounding-box))
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
                (frame-h/zoom-in-pointer-position db factor))
              (frame-h/pan db delta))

            db)
      :fx [(case (:type e)
             :drop
             [:data-transfer [adjusted-pointer-pos data-transfer]]

             :pointerdown
             [:set-pointer-capture [(:target e) (:pointer-id e)]]

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
 :focus
 (fn [_ [_ id]]
   {:focus id}))

(rf/reg-event-fx
 :load-system-fonts
 (fn [_ [_ file-path]]
   {:ipc-invoke ["load-system-fonts"
                 file-path
                 #(rf/dispatch [:set-system-fonts (js->clj % :keywordize-keys true)])]}))

(rf/reg-event-fx
 :load-webref
 (fn [_ [_ file-path]]
   {:ipc-invoke ["load-webref"
                 file-path
                 #(rf/dispatch [:set-webref-css (js->clj % :keywordize-keys true)])]}))
