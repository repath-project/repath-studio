(ns renderer.utils.wheel
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.utils.keyboard :refer [ModifierKey]]
   [renderer.utils.math :refer [Vec2D]]))

(def WheelEvent
  [:map {:closed true}
   [:target any?]
   [:type [:= "wheel"]]
   [:pointer-pos [:maybe Vec2D]]
   [:delta [:maybe Vec2D]]
   [:modifiers [:set ModifierKey]]])

(defn event-handler
  "Gathers wheel event props.
   https://developer.mozilla.org/en-US/docs/Web/API/WheelEvent"
  [^js/PointerEvent e]
  (.stopPropagation e)

  (when (.-ctrlKey e) (.preventDefault e)) ; Disable wheel zoom on canvas.

  (rf/dispatch-sync [::app.e/wheel-event {:target (.-target e)
                                          :type (.-type e)
                                          :pointer-pos [(.-pageX e) (.-pageY e)]
                                          :delta [(.-deltaX e) (.-deltaY e)]
                                          :modifiers (cond-> #{}
                                                       (.-altKey e) (conj :alt)
                                                       (.-ctrlKey e) (conj :ctrl)
                                                       (.-metaKey e) (conj :meta)
                                                       (.-shiftKey e) (conj :shift))}]))
