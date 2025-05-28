(ns renderer.event.impl.drag
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.event.db :refer [DragEvent]]
   [renderer.event.events :as-alias event.events]))

(m/=> ->clj [:-> any? DragEvent])
(defn ->clj
  "https://developer.mozilla.org/en-US/docs/Web/API/DragEvent"
  [^js/DragEvent e]
  {:type (.-type e)
   :pointer-pos [(.-pageX e) (.-pageY e)]
   :data-transfer (.-dataTransfer e)})

(defn handler!
  [^js/DragEvent e]
  (.stopPropagation e)
  (.preventDefault e)

  (rf/dispatch-sync [::event.events/drag (->clj e)]))
