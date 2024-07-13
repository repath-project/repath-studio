(ns renderer.events
  (:require
   [malli.core :as ma]
   [platform]
   [re-frame.core :as rf]
   [renderer.db :as db]
   [renderer.handlers :as h]
   [renderer.frame.handlers :as frame-h]
   [renderer.tool.base :as tool]
   [renderer.utils.dom :as dom]
   [renderer.utils.drop :as drop]
   [renderer.utils.local-storage :as local-storage]
   [renderer.utils.pointer :as pointer]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec"
  [spec db]
  (when-not (ma/validate spec db)
    (js/console.log (ex-info (str "spec check failed: " (ma/explain spec db)) {}))
    db))

#_:clj-kondo/ignore
(def schema-valdator (rf/after (partial check-and-throw db/app)))

#_(rf/reg-global-interceptor schema-valdator)

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

(rf/reg-event-db
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
   (update db :rulers? not)))

(rf/reg-event-db
 :toggle-rulers-locked
 (fn [db [_]]
   (update db :rulers-locked? not)))

(rf/reg-event-db
 :toggle-grid
 (fn [db [_]]
   (update db :grid? not)))

(rf/reg-event-db
 :panel/toggle
 [local-storage/persist
  (rf/path :panel)]
 (fn [db [_ key]]
   (update-in db [key :visible?] not)))

(rf/reg-event-fx
 :theme/init-mode
 (fn [{:keys [db]} _]
   (let [mode (-> db :theme :mode name)]
     {:set-attribute [js/window.document.documentElement "data-theme" mode]
      :send-to-main {:action "setThemeMode" :data mode}})))

(rf/reg-event-fx
 :theme/set-mode
 local-storage/persist
 (fn [{:keys [db]} [_ mode]]
   {:db (assoc-in db [:theme :mode] mode)
    :dispatch [:theme/init-mode]}))

(rf/reg-event-fx
 :theme/cycle-mode
 (fn [{:keys [db]} [_]]
   (let [mode (case (-> db :theme :mode)
                ;; TODO: Support system mode.
                :dark :light
                :light :dark)]
     {:dispatch [:theme/set-mode mode]})))

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
             [:drop [adjusted-pointer-pos data-transfer]]

             :pointerdown
             [:set-pointer-capture (:pointer-id e)]

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

(rf/reg-fx
 :send-to-main
 (fn [data]
   (when platform/electron?
     (js/window.api.send "toMain" (clj->js data)))))

(rf/reg-fx
 :drop
 (fn [[position data-transfer]]
   (drop/items! position (.-items data-transfer))
   (drop/files! position (.-files data-transfer))))


(rf/reg-fx
 :set-pointer-capture
 (fn [pointer-id]
   (when-let [canvas (dom/canvas-element)]
     (.setPointerCapture canvas pointer-id))))

(rf/reg-fx
 :clipboard-write
 (fn [text-html]
   (js/navigator.clipboard.write
    (clj->js [(js/ClipboardItem.
               #js {"text/html" (when text-html
                                  (js/Blob.
                                   [text-html]
                                   #js {:type ["text/html"]}))})]))))

(rf/reg-fx
 :focus
 (fn [id]
   (when-let [element (if id (.getElementById js/document id) (dom/canvas-element))]
     (js/setTimeout #(.focus element)))))

(rf/reg-event-fx
 :focus
 (fn [_ [_ id]]
   {:focus id}))

(rf/reg-fx
 :set-attribute
 (fn [[el attr val]]
   (.setAttribute el attr val)))
