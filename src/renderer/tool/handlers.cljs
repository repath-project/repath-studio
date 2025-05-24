(ns renderer.tool.handlers
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.app.effects :as-alias app.effects]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.handlers]
   [renderer.event.keyboard :refer [KeyboardEvent]]
   [renderer.event.pointer :refer [PointerEvent]]
   [renderer.event.wheel :refer [WheelEvent]]
   [renderer.frame.db :refer [DomRect]]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.db :refer [Tool State Cursor]]
   [renderer.tool.effects :as-alias tool.effects]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.element :as utils.element]
   [renderer.utils.math :refer [Vec2]]))

(m/=> add-fx [:-> App vector? App])
(defn add-fx
  [db effect]
  (update db :fx conj effect))

(m/=> set-state [:-> App State App])
(defn set-state
  [db state]
  (assoc db :state state))

(m/=> set-cursor [:-> App Cursor App])
(defn set-cursor
  [db cursor]
  (assoc db :cursor cursor))

(m/=> activate [:-> App Tool App])
(defn activate
  [db tool]
  (-> db
      (tool.hierarchy/on-deactivate)
      (assoc :tool tool)
      (set-state :idle)
      (set-cursor "default")
      (dissoc :drag :pointer-offset :clicked-element)
      (snap.handlers/rebuild-tree)
      (tool.hierarchy/on-activate)))

(m/=> pointer-delta [:-> App Vec2])
(defn pointer-delta
  [db]
  (matrix/sub (:adjusted-pointer-pos db) (:adjusted-pointer-offset db)))

(m/=> significant-drag? [:-> Vec2 Vec2 number? boolean?])
(defn significant-drag?
  [position offset threshold]
  (> (apply max (map abs (matrix/sub position offset)))
     threshold))

(m/=> dissoc-temp [:-> App App])
(defn dissoc-temp
  [db]
  (cond-> db
    (:active-document db)
    (update-in [:documents (:active-document db)] dissoc :temp-element)))

(m/=> set-temp [:-> App map? App])
(defn set-temp
  [db el]
  (->> (utils.element/normalize-attrs el)
       (assoc-in db [:documents (:active-document db) :temp-element])))

(m/=> temp [:-> App [:maybe Element]])
(defn temp
  [db]
  (get-in db [:documents (:active-document db) :temp-element]))

(m/=> create-temp-element [:-> App App])
(defn create-temp-element
  [db]
  (->> (temp db)
       (element.handlers/add db)
       (dissoc-temp)))

(m/=> axis-pan-offset [:-> number? number? number? number?])
(defn axis-pan-offset
  [position offset size]
  (let [threshold 50
        step 15]
    (cond
      (and (< position threshold)
           (< position offset))
      (- step)

      (and (> position (- size threshold))
           (> position offset))
      step

      :else 0)))

(m/=> pan-out-of-canvas [:-> App DomRect Vec2 Vec2 App])
(defn pan-out-of-canvas
  [db dom-rect pointer-pos pointer-offset]
  (let [[x y] pointer-pos
        [offset-x offset-y] pointer-offset
        pan [(axis-pan-offset x offset-x (:width dom-rect))
             (axis-pan-offset y offset-y (:height dom-rect))]]
    (cond-> db
      (not-every? zero? pan)
      (-> (frame.handlers/pan-by pan)
          ; REVIEW: Can we improve performance?
          (snap.handlers/update-viewport-tree)))))

(m/=> pointer-handler [:-> App PointerEvent number? App])
(defn pointer-handler
  [db e]
  (let [{:keys [pointer-offset tool dom-rect drag primary-tool drag-threshold nearest-neighbor]} db
        {:keys [button pointer-pos timestamp]} e
        adjusted-pointer-pos (frame.handlers/adjusted-pointer-pos db pointer-pos)
        db (snap.handlers/update-nearest-neighbors db)]
    (case (:type e)
      "pointermove"
      (-> (if pointer-offset
            (if (significant-drag? pointer-pos pointer-offset drag-threshold)
              (cond-> db
                (not= tool :pan)
                (pan-out-of-canvas dom-rect pointer-pos pointer-offset)

                (not drag)
                (-> (tool.hierarchy/on-drag-start e)
                    (add-fx [::tool.effects/set-pointer-capture (:pointer-id e)])
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
            (activate :pan))

        (not= button :right)
        (assoc :pointer-offset pointer-pos
               :adjusted-pointer-offset adjusted-pointer-pos
               :nearest-neighbor-offset (:point nearest-neighbor))

        :always
        (tool.hierarchy/on-pointer-down e))

      "pointerup"
      (cond-> (if drag
                (-> (tool.hierarchy/on-drag-end db e)
                    (add-fx [::tool.effects/release-pointer-capture (:pointer-id e)]))
                (if (= button :right)
                  db
                  (if (> (- timestamp (:event-time db)) (:double-click-delta db))
                    (-> (assoc db :event-time timestamp)
                        (tool.hierarchy/on-pointer-up e))
                    (tool.hierarchy/on-double-click db e))))
        (and primary-tool (= button :middle))
        (-> (activate primary-tool)
            (dissoc :primary-tool))

        :always
        (dissoc :pointer-offset :drag :nearest-neighbor))

      db)))

(m/=> key-handler [:-> App KeyboardEvent App])
(defn key-handler
  [db e]
  (case (:type e)
    "keydown"
    (cond-> db
      (and (= (:code e) "Space")
           (not= (:tool db) :pan))
      (-> (assoc :primary-tool (:tool db))
          (activate :pan))

      :always
      (tool.hierarchy/on-key-down e))

    "keyup"
    (cond-> db
      (and (= (:code e) "Space")
           (:primary-tool db))
      (-> (activate (:primary-tool db))
          (dissoc :primary-tool))

      :always
      (tool.hierarchy/on-key-up e))

    db))

(m/=> wheel-handler [:-> App WheelEvent App])
(defn wheel-handler
  [db e]
  (-> (if (or (:ctrl-key e) (:shift-key e))
        (let [factor (Math/pow (inc (/ (- 1 (:zoom-sensitivity db)) 100))
                               (- (:delta-y e)))]
          (frame.handlers/zoom-at-pointer db factor))
        (frame.handlers/pan-by db [(:delta-x e) (:delta-y e)]))
      (snap.handlers/update-viewport-tree)
      (add-fx [::app.effects/persist])))

(m/=> cancel [:-> App App])
(defn cancel
  [db]
  (cond-> db
    :always
    (-> (activate (:tool db))
        (dissoc-temp)
        (history.handlers/reset-state))

    (= (:state db) :idle)
    (activate :transform)

    :always
    (set-state :idle)))
