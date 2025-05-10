(ns renderer.utils.wheel
  (:require
   [malli.core :as m]
   [renderer.tool.events :as-alias tool.events]
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

(m/=> event-formatter [:-> any? WheelEvent])
(defn event-formatter
  "Gathers wheel event props.
   https://developer.mozilla.org/en-US/docs/Web/API/WheelEvent"
  [^js/WheelEvent e]
  {:target (.-target e)
   :type (.-type e)
   :pointer-pos [(.-pageX e) (.-pageY e)]
   :delta-x (.-deltaX e)
   :delta-y (.-deltaY e)
   :delta-z (.-deltaZ e)
   :modifiers (modifiers e)})
