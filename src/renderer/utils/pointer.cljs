(ns renderer.utils.pointer
  (:require
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.utils.math :as math]))

(defn ctrl?
  [e]
  (contains? (:modifiers e) :ctrl))

(defn shift?
  [e]
  (contains? (:modifiers e) :shift))

(defn alt?
  [e]
  (contains? (:modifiers e) :alt))

(mx/defn significant-drag? :- boolean?
  [position :- math/vec2d, offset :- math/vec2d, threshold :- number?]
  (> (apply max (map abs (mat/sub position offset)))
     threshold))

(mx/defn adjust-position :- math/vec2d
  [zoom :- number?, pan :- math/vec2d, pointer-pos :- math/vec2d]
  (-> pointer-pos
      (mat/div zoom)
      (mat/add pan)))

(def button
  "https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/button"
  {0 :left
   1 :middle
   2 :right
   3 :back
   4 :forward})

(mx/defn lock-direction :- math/vec2d
  "Locks pointer movement to the axis with the biggest offset"
  [[x y] :- math/vec2d]
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
            (= (.-type e) "drop")
            (= (.-pointerType e) "touch"))
    (.preventDefault e))

  (rf/dispatch-sync [::app.e/pointer-event {:element el
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
