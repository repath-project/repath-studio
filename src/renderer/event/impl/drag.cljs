(ns renderer.event.impl.drag
  (:require
   [re-frame.core :as rf]
   [renderer.event.events :as-alias event.events]))

(defn handler!
  "https://developer.mozilla.org/en-US/docs/Web/API/DragEvent"
  [^js/DragEvent e]
  (.stopPropagation e)
  (.preventDefault e)

  (rf/dispatch-sync [::event.events/drag {:type (.-type e)
                                          :pointer-pos [(.-pageX e) (.-pageY e)]
                                          :data-transfer (.-dataTransfer e)}]))
