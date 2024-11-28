(ns renderer.utils.wheel
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.tool.events :as-alias tool.e]
   [renderer.utils.keyboard :refer [ModifierKey modifiers]]
   [renderer.utils.math :refer [Vec2]]))

(def WheelEvent
  [:map {:closed true}
   [:target any?]
   [:type [:= "wheel"]]
   [:pointer-pos [:maybe Vec2]]
   [:delta-x [:maybe number?]]
   [:delta-y [:maybe number?]]
   [:delta-z [:maybe number?]]
   [:modifiers [:set ModifierKey]]])

(m/=> event-handler! [:-> any? nil?])
(defn event-handler!
  "Gathers wheel event props.
   https://developer.mozilla.org/en-US/docs/Web/API/WheelEvent"
  [^js/WheelEvent e]
  (.stopPropagation e)

  (when (.-ctrlKey e) (.preventDefault e)) ; Disable wheel zoom on canvas.

  (rf/dispatch-sync [::tool.e/wheel-event {:target (.-target e)
                                           :type (.-type e)
                                           :pointer-pos [(.-pageX e) (.-pageY e)]
                                           :delta-x (.-deltaX e)
                                           :delta-y (.-deltaY e)
                                           :delta-z (.-deltaZ e)
                                           :modifiers (modifiers e)}]))
