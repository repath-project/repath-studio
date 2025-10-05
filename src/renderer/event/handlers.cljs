(ns renderer.event.handlers
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.handlers :as app.handlers]
   [renderer.db :refer [Vec2]]
   [renderer.effects :as-alias effects]
   [renderer.event.db :refer [PointerEvent KeyboardEvent WheelEvent DragEvent]]
   [renderer.event.effects :as-alias event.effects]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(m/=> significant-drag? [:-> Vec2 Vec2 number? boolean?])
(defn significant-drag?
  [position offset threshold]
  (> (apply max (map abs (matrix/sub position offset)))
     threshold))

(m/=> pointer [:-> App PointerEvent App])
(defn pointer
  [db e]
  (let [{:keys [pointer-offset tool state cached-tool cached-state
                dom-rect drag drag-threshold nearest-neighbor active-pointers
                double-click-delta event-timestamp]} db
        {:keys [button pointer-pos timestamp pointer-id]} e
        adjusted-pointer-pos (frame.handlers/adjusted-pointer-pos db pointer-pos)
        db (snap.handlers/update-nearest-neighbors db)]
    (case (:type e)
      "pointermove"
      (-> (if pointer-offset
            (if (significant-drag? pointer-pos pointer-offset drag-threshold)
              (cond-> db
                (not= tool :pan)
                (tool.handlers/pan-out-of-canvas dom-rect
                                                 pointer-pos
                                                 pointer-offset)

                (not drag)
                (-> (assoc :drag true)
                    (tool.hierarchy/on-drag-start e)
                    (app.handlers/add-fx [::event.effects/set-pointer-capture
                                          pointer-id]))

                :always
                (tool.hierarchy/on-drag e))
              db)
            (tool.hierarchy/on-pointer-move db e))
          (assoc :pointer-pos pointer-pos
                 :adjusted-pointer-pos adjusted-pointer-pos))

      "pointerdown"
      (cond-> db
        (= button :middle)
        (-> (assoc :cached-tool tool
                   :cached-state state)
            (tool.handlers/activate :pan))

        (not= button :right)
        (-> (update :active-pointers conj pointer-id)
            (assoc :pointer-offset pointer-pos
                   :adjusted-pointer-offset adjusted-pointer-pos
                   :nearest-neighbor-offset (:point nearest-neighbor)))
        :always
        (-> (tool.hierarchy/on-pointer-down e)
            (app.handlers/add-fx [::effects/focus nil])))

      "pointerup"
      (if (contains? active-pointers pointer-id)
        (cond-> (if drag
                  (-> (tool.hierarchy/on-drag-end db e)
                      (app.handlers/add-fx [::event.effects/release-pointer-capture
                                            pointer-id]))
                  (if (= button :right)
                    db
                    (if (< 0 (- timestamp event-timestamp) double-click-delta)
                      (-> (dissoc db :event-timestamp)
                          (tool.hierarchy/on-double-click e))
                      (-> (assoc db :event-timestamp timestamp)
                          (tool.hierarchy/on-pointer-up e)))))
          (and cached-tool (= button :middle))
          (-> (tool.handlers/activate cached-tool)
              (tool.handlers/set-state cached-state)
              (dissoc :cached-tool :cached-state))

          :always
          (-> (update :active-pointers disj pointer-id)
              (dissoc :pointer-offset :drag :nearest-neighbor)))
        db)
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
      (-> (assoc :cached-tool (:tool db))
          (tool.handlers/activate :pan))

      (= (:key e) "Shift")
      (-> (assoc-in [:snap :transient-active] true)
          (cond->
           (not (-> db :snap :active))
            (-> (dissoc :nearest-neighbor)
                (snap.handlers/rebuild-tree))))

      :always
      (tool.hierarchy/on-key-down e))

    "keyup"
    (cond-> db
      (and (= (:code e) "Space")
           (:cached-tool db))
      (-> (tool.handlers/activate (:cached-tool db))
          (dissoc :cached-tool))

      (= (:key e) "Shift")
      (-> (assoc-in [:snap :transient-active] false)
          (cond->
           (not (-> db :snap :active))
            (dissoc :nearest-neighbor)))

      :always
      (tool.hierarchy/on-key-up e))
    db))

(m/=> wheel [:-> App WheelEvent App])
(defn wheel
  [db e]
  (let [{:keys [delta-x delta-y ctrl-key shift-key]} e]
    (-> (if (or ctrl-key shift-key)
          (let [factor (-> (:zoom-sensitivity db)
                           dec (/ 100) inc
                           (Math/pow delta-y))]
            (frame.handlers/zoom-at-pointer db factor))
          (frame.handlers/pan-by db [delta-x delta-y]))
        (snap.handlers/update-viewport-tree)
        (app.handlers/add-fx [::app.effects/persist]))))

(m/=> drag [:-> App DragEvent App])
(defn drag
  [db e]
  (case (:type e)
    "drop"
    (let [{:keys [data-transfer pointer-pos]} e
          position (frame.handlers/adjusted-pointer-pos db pointer-pos)]
      (app.handlers/add-fx db [::event.effects/drop [position data-transfer]]))

    db))
