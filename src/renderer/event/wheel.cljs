(ns renderer.event.wheel
  (:require
   [malli.core :as m]
   [renderer.utils.math :refer [Vec2]]))

(def WheelEvent
  [:map {:closed true}
   [:target any?]
   [:type [:= "wheel"]]
   [:pointer-pos [:maybe Vec2]]
   [:delta-x [:maybe number?]]
   [:delta-y [:maybe number?]]
   [:delta-z [:maybe number?]]
   [:alt-key boolean?]
   [:ctrl-key boolean?]
   [:meta-key boolean?]
   [:shift-key boolean?]])

(m/=> ->map [:-> any? WheelEvent])
(defn ->map
  "https://developer.mozilla.org/en-US/docs/Web/API/WheelEvent"
  [^js/WheelEvent e]
  {:target (.-target e)
   :type (.-type e)
   :pointer-pos [(.-pageX e) (.-pageY e)]
   :delta-x (.-deltaX e)
   :delta-y (.-deltaY e)
   :delta-z (.-deltaZ e)
   :alt-key (.-altKey e)
   :ctrl-key (.-ctrlKey e)
   :meta-key (.-metaKey e)
   :shift-key (.-shiftKey e)})
