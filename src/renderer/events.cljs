(ns renderer.events
  (:require
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]
   [clojure.core.matrix :as matrix]
   [renderer.frame.handlers :as frame-handlers]
   [renderer.handlers :as handlers]
   [renderer.utils.local-storage :as local-storage]
   [renderer.db :as db]
   [malli.core :as ma]))

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
 (fn  [_ _]
   db/default))

(rf/reg-event-db
 :load-local-db
 local-storage/persist
 (fn [db _]
   db))

(rf/reg-event-db
 :set-active-document
 (fn [db [_ document-id]]
   (assoc db :active-document document-id)))

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

(rf/reg-event-db
 :set-command-palette?
 (fn [db [_ visible?]]
   (assoc db :command-palette? visible?)))

(rf/reg-event-db
 :set-tool
 (fn [db [_ tool]]
   (tools/set-tool db tool)))

#_(rf/reg-event-db
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

(defn significant-movement?
  [mouse-pos mouse-offset]
  (let [threshold 1]
    (when (and (vector? mouse-pos) (vector? mouse-offset))
      (> (apply max (map abs (matrix/sub mouse-pos mouse-offset)))
         threshold))))

(rf/reg-event-db
 :pointer-event
 (fn [{:keys [mouse-offset tool content-rect] :as db} [_ event]]
   (let [{:keys [mouse-pos delta element]} event
         mouse-pos (mapv js/parseInt mouse-pos)
         adjusted-mouse-pos (frame-handlers/adjusted-mouse-pos db mouse-pos)]
     (case (:type event)
       :pointermove
       (-> (if (and (significant-movement? mouse-pos mouse-offset)
                    (not (= (:buttons event) 2)))
             (cond-> db
               (not= tool :pan)
               (frame-handlers/pan-out-of-canvas content-rect
                                                 mouse-pos
                                                 mouse-offset)

               (not (:drag? db))
               (-> (tools/drag-start event element)
                   (assoc :drag? true))

               :always
               (tools/drag event element))
             (tools/mouse-move db event element))
           (assoc :mouse-pos mouse-pos
                  :adjusted-mouse-pos adjusted-mouse-pos))

       :pointerdown
       (cond-> db
         (= (:button event) 1)
         (-> (assoc :primary-tool tool)
             (tools/set-tool :pan))

         :always
         (-> (tools/mouse-down event element)
             (assoc :mouse-offset mouse-pos
                    :adjusted-mouse-offset adjusted-mouse-pos)))

       :pointerup
       (cond-> (if (:drag? db)
                 (tools/drag-end db event element)
                 (tools/mouse-up db event element))
         (and (:primary-tool db) (= (:button event) 1))
         (-> (tools/set-tool (:primary-tool db))
             (dissoc :primary-tool))

         :always
         (dissoc :mouse-offset :drag?))

       :dblclick
       (tools/double-click db event element)

       :wheel
       (if (some (:modifiers event) [:ctrl :alt])
         (let [factor (if (pos? (second delta))
                        (:zoom-factor db)
                        (/ 1 (:zoom-factor db)))]
           (frame-handlers/zoom-in-mouse-position db factor))
         (frame-handlers/pan db delta))

       :drop
       (let [data-transfer (:data-transfer event)
             items (.-items data-transfer)
             files (.-files data-transfer)]
         (-> db
             (assoc :mouse-pos mouse-pos
                    :adjusted-mouse-pos adjusted-mouse-pos)
             (cond->
              items (handlers/drop-items items)
              files (handlers/drop-files files))))

       db))))

(rf/reg-event-db
 :keyboard-event
 (fn [{:keys [tool] :as db} [_ {:keys [type code] :as event}]]
   (case type
     :keydown
     (cond-> db
       (and (= code "Space")
            (not= tool :pan))
       (-> (assoc :primary-tool tool)
           (tools/set-tool :pan))

       :always
       (tools/key-down event))

     :keyup
     (cond-> db
       (and (= code "Space")
            (:primary-tool db))
       (-> (tools/set-tool (:primary-tool db))
           (dissoc :primary-tool))

       :always
       (tools/key-up event))

     db)))