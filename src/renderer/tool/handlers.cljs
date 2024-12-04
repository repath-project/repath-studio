(ns renderer.tool.handlers
  (:require
   [clojure.core.matrix :as mat]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.app.effects :as-alias app.fx]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.h]
   [renderer.frame.db :refer [DomRect]]
   [renderer.frame.handlers :as frame.h]
   [renderer.history.handlers :as history.h]
   [renderer.snap.handlers :as snap.h]
   [renderer.tool.db :refer [Tool State Cursor]]
   [renderer.tool.effects :as-alias fx]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.element :as element]
   [renderer.utils.keyboard :refer [KeyboardEvent]]
   [renderer.utils.math :as math :refer [Vec2]]
   [renderer.utils.pointer :as pointer :refer [PointerEvent]]
   [renderer.utils.wheel :refer [WheelEvent]]))

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
      (hierarchy/on-deactivate)
      (assoc :tool tool)
      (set-state :idle)
      (set-cursor "default")
      (dissoc :drag :pointer-offset :clicked-element)
      (snap.h/rebuild-tree)
      (hierarchy/on-activate)))

(m/=> pointer-delta [:-> App Vec2])
(defn pointer-delta
  [db]
  (mat/sub (:adjusted-pointer-pos db) (:adjusted-pointer-offset db)))

(m/=> significant-drag? [:-> Vec2 Vec2 number? boolean?])
(defn significant-drag?
  [position offset threshold]
  (> (apply max (map abs (mat/sub position offset)))
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
  (->> (element/normalize-attrs el)
       (assoc-in db [:documents (:active-document db) :temp-element])))

(m/=> temp [:-> App [:maybe Element]])
(defn temp
  [db]
  (get-in db [:documents (:active-document db) :temp-element]))

(m/=> create-temp-element [:-> App App])
(defn create-temp-element
  [db]
  (->> (temp db)
       (element.h/add db)
       (dissoc-temp)))

(m/=> axis-offset [:-> number? number? number? number?])
(defn axis-offset
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
        pan [(axis-offset x offset-x (:width dom-rect))
             (axis-offset y offset-y (:height dom-rect))]]
    (cond-> db
      (not-every? zero? pan)
      (-> (frame.h/pan-by pan)
          ; REVIEW: Can we improve performance?
          (snap.h/update-viewport-tree)))))

(m/=> pointer-handler [:-> App PointerEvent number? App])
(defn pointer-handler
  [db e timestamp]
  (let [{:keys [pointer-offset tool dom-rect drag primary-tool drag-threshold nearest-neighbor]} db
        {:keys [button pointer-pos]} e
        adjusted-pointer-pos (frame.h/adjusted-pointer-pos db pointer-pos)
        db (snap.h/update-nearest-neighbors db)]
    (case (:type e)
      "pointermove"
      (-> (if pointer-offset
            (if (significant-drag? pointer-pos pointer-offset drag-threshold)
              (cond-> db
                (not= tool :pan)
                (pan-out-of-canvas dom-rect pointer-pos pointer-offset)

                (not drag)
                (-> (hierarchy/on-drag-start e)
                    (add-fx [::fx/set-pointer-capture (:pointer-id e)])
                    (assoc :drag true))

                :always
                (hierarchy/on-drag e))
              db)
            (hierarchy/on-pointer-move db e))
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
        (hierarchy/on-pointer-down e))

      "pointerup"
      (cond-> (if drag
                (-> (hierarchy/on-drag-end db e)
                    (add-fx [::fx/release-pointer-capture (:pointer-id e)]))
                (if (= button :right)
                  db
                  (if (> (- timestamp (:event-time db)) (:double-click-delta db))
                    (-> (assoc db :event-time timestamp)
                        (hierarchy/on-pointer-up e))
                    (hierarchy/on-double-click db e))))
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
      (hierarchy/on-key-down e))

    "keyup"
    (cond-> db
      (and (= (:code e) "Space")
           (:primary-tool db))
      (-> (activate (:primary-tool db))
          (dissoc :primary-tool))

      :always
      (hierarchy/on-key-up e))

    db))

(m/=> wheel-handler [:-> App WheelEvent App])
(defn wheel-handler
  [db e]
  (-> (if (some (:modifiers e) [:ctrl :alt])
        (let [factor (Math/pow (inc (/ (- 1 (:zoom-sensitivity db)) 100))
                               (- (:delta-y e)))]
          (frame.h/zoom-at-pointer db factor))
        (frame.h/pan-by db [(:delta-x e) (:delta-y e)]))
      (snap.h/update-viewport-tree)
      (add-fx [::app.fx/persist])))

(m/=> cancel [:-> App App])
(defn cancel
  [db]
  (cond-> db
    :always
    (-> (activate (:tool db))
        (dissoc-temp)
        (history.h/reset-state))

    (= (:state db) :idle)
    (activate :transform)

    :always
    (set-state :idle)))
