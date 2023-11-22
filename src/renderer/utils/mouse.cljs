(ns renderer.utils.mouse
  (:require
   [re-frame.core :as rf]))

(defn multiselect?
  [event]
  (some #(contains? (:modifiers event) %) #{:ctrl :shift}))

(defn lock-direction
  "Locks mouse movement to the axis with the biggest offset"
  [[x y]]
  (if (> (abs x) (abs y))
    [x 0]
    [0 y]))

(defn event-handler
  "Gathers pointer event props.
   https://developer.mozilla.org/en-US/docs/Web/API/PointerEvent
   
   Then dispathces the corresponding event.
   SEE https://day8.github.io/re-frame/FAQs/Null-Dispatched-Events/
   
   Although the fps might drop because synced dispatch blocks the rendering, 
   the end result appears to be more responsive because it's synced with the 
   mouse movement."
  [event element]
  (.stopPropagation event)
  ;; Disable native zoom on canvas
  (when (and (.-ctrlKey event) (.-deltaY event))
    (.preventDefault event))

  (rf/dispatch-sync [:pointer-event {:element element
                                     :target (.-target event)
                                     :type (keyword (.-type event))
                                     :mouse-pos [(.-pageX event) (.-pageY event)]
                                     :pressure (.-pressure event)
                                     :pointer-type (.-pointerType event)
                                     :primary? (.-isPrimary event)
                                     :altitude (.-altitudeAngle event)
                                     :azimuth (.-azimuthAngle event)
                                     :twist (.-twist event)
                                     :tilt [(.-tiltX event) (.-tiltY event)]
                                     :data-transfer (.-dataTransfer event)
                                     :button (.-button event)
                                     :buttons (.-buttons event)
                                     :delta [(.-deltaX event) (.-deltaY event)]
                                     :modifiers (cond-> #{}
                                                  (.-altKey event) (conj :alt)
                                                  (.-ctrlKey event) (conj :ctrl)
                                                  (.-metaKey event) (conj :meta)
                                                  (.-shiftKey event) (conj :shift))}]))
