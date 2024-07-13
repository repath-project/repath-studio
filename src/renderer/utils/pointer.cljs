(ns renderer.utils.pointer
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]))

(defn ctrl?
  [e]
  (contains? (:modifiers e) :ctrl))

(defn shift?
  [e]
  (contains? (:modifiers e) :shift))

(defn significant-drag?
  [pointer-pos pointer-offset]
  (let [threshold 1]
    (when (and (vector? pointer-pos) (vector? pointer-offset))
      (> (apply max (map abs (mat/sub pointer-pos pointer-offset)))
         threshold))))

(defn adjust-position
  [zoom pan pointer-pos]
  (-> pointer-pos
      (mat/div zoom)
      (mat/add pan)))

(def button
  "https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/button"
  [:left
   :middle
   :right
   :back
   :forward])

(defn lock-direction
  "Locks pointer movement to the axis with the biggest offset"
  [[x y]]
  (if (> (abs x) (abs y))
    [x 0]
    [0 y]))

(defn event-handler
  "Gathers pointer event props.
   https://developer.mozilla.org/en-US/docs/Web/API/PointerEvent

   Then dispathces the corresponding event.
   https://day8.github.io/re-frame/FAQs/Null-Dispatched-Events/

   Although the fps might drop because synced dispatch blocks the rendering,
   the end result appears to be more responsive because it's synced with the
   pointer movement."
  [^js/PointerEvent e el]
  (.stopPropagation e)
  ;; Disable zoom and drop handling on canvas.
  (when (or (and (.-ctrlKey e) (.-deltaY e))
            (= (.-type e) "drop"))
    (.preventDefault e))

  (rf/dispatch-sync [:pointer-event {:element el
                                     :target (.-target e)
                                     :type (keyword (.-type e))
                                     :pointer-pos [(.-pageX e) (.-pageY e)]
                                     :pressure (.-pressure e)
                                     :pointer-type (.-pointerType e)
                                     :pointer-id (.-pointerId e)
                                     :primary? (.-isPrimary e)
                                     :altitude (.-altitudeAngle e)
                                     :azimuth (.-azimuthAngle e)
                                     :twist (.-twist e)
                                     :tilt [(.-tiltX e) (.-tiltY e)]
                                     :data-transfer (.-dataTransfer e)
                                     :button (get button (.-button e))
                                     :buttons (get button (.-buttons e))
                                     :delta [(.-deltaX e) (.-deltaY e)]
                                     :modifiers (cond-> #{}
                                                  (.-altKey e) (conj :alt)
                                                  (.-ctrlKey e) (conj :ctrl)
                                                  (.-metaKey e) (conj :meta)
                                                  (.-shiftKey e) (conj :shift))}]))
