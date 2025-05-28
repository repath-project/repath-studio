(ns renderer.event.handlers
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.app.effects :as-alias app.effects]
   [renderer.event.db :refer [PointerEvent KeyboardEvent WheelEvent DragEvent]]
   [renderer.event.effects :as-alias event.effects]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.math :refer [Vec2]]))

(m/=> significant-drag? [:-> Vec2 Vec2 number? boolean?])
(defn significant-drag?
  [position offset threshold]
  (> (apply max (map abs (matrix/sub position offset)))
     threshold))

(m/=> pointer [:-> App PointerEvent number? App])
(defn pointer
  [db e]
  (let [{:keys [pointer-offset tool dom-rect drag primary-tool drag-threshold nearest-neighbor]} db
        {:keys [button pointer-pos timestamp pointer-id]} e
        adjusted-pointer-pos (frame.handlers/adjusted-pointer-pos db pointer-pos)
        db (snap.handlers/update-nearest-neighbors db)]
    (case (:type e)
      "pointermove"
      (-> (if pointer-offset
            (if (significant-drag? pointer-pos pointer-offset drag-threshold)
              (cond-> db
                (not= tool :pan)
                (tool.handlers/pan-out-of-canvas dom-rect pointer-pos pointer-offset)

                (not drag)
                (-> (tool.hierarchy/on-drag-start e)
                    (tool.handlers/add-fx [::event.effects/set-pointer-capture pointer-id])
                    (assoc :drag true))

                :always
                (tool.hierarchy/on-drag e))
              db)
            (tool.hierarchy/on-pointer-move db e))
          (assoc :pointer-pos pointer-pos
                 :adjusted-pointer-pos adjusted-pointer-pos))

      "pointerdown"
      (cond-> db
        (= button :middle)
        (-> (assoc :primary-tool tool)
            (tool.handlers/activate :pan))

        (not= button :right)
        (assoc :pointer-offset pointer-pos
               :adjusted-pointer-offset adjusted-pointer-pos
               :nearest-neighbor-offset (:point nearest-neighbor))

        :always
        (tool.hierarchy/on-pointer-down e))

      "pointerup"
      (cond-> (if drag
                (-> (tool.hierarchy/on-drag-end db e)
                    (tool.handlers/add-fx [::event.effects/release-pointer-capture pointer-id]))
                (if (= button :right)
                  db
                  (if (< 0 (- timestamp (:event-timestamp db)) (:double-click-delta db))
                    (-> (dissoc db :event-timestamp)
                        (tool.hierarchy/on-double-click e))
                    (-> (assoc db :event-timestamp timestamp)
                        (tool.hierarchy/on-pointer-up e)))))
        (and primary-tool (= button :middle))
        (-> (tool.handlers/activate primary-tool)
            (dissoc :primary-tool))

        :always
        (dissoc :pointer-offset :drag :nearest-neighbor))

      db)))

(m/=> keyboard [:-> App KeyboardEvent App])
(defn keyboard
  [db e]
  (case (:type e)
    "keydown"
    (cond-> db
      (and (= (:code e) "Space")
           (not= (:tool db) :pan)
           (= (:state db) :idle))
      (-> (assoc :primary-tool (:tool db))
          (tool.handlers/activate :pan))

      :always
      (tool.hierarchy/on-key-down e))

    "keyup"
    (cond-> db
      (and (= (:code e) "Space")
           (:primary-tool db))
      (-> (tool.handlers/activate (:primary-tool db))
          (dissoc :primary-tool))

      :always
      (tool.hierarchy/on-key-up e))

    db))

(m/=> wheel [:-> App WheelEvent App])
(defn wheel
  [db e]
  (-> (if (or (:ctrl-key e) (:shift-key e))
        (let [factor (Math/pow (inc (/ (- 1 (:zoom-sensitivity db)) 100))
                               (- (:delta-y e)))]
          (frame.handlers/zoom-at-pointer db factor))
        (frame.handlers/pan-by db [(:delta-x e) (:delta-y e)]))
      (snap.handlers/update-viewport-tree)
      (tool.handlers/add-fx [::app.effects/persist])))

(m/=> drag [:-> App DragEvent App])
(defn drag
  [db e]
  (case (:type e)
    "drop"
    (let [{:keys [data-transfer pointer-pos]} e
          position (frame.handlers/adjusted-pointer-pos db pointer-pos)]
      (tool.handlers/add-fx db [::event.effects/drop [position data-transfer]]))

    db))
