(ns repath.studio.mouse
  (:require
   [re-frame.core :as rf]))

(defn event-handler
  [event element]
  (when event
    (.stopPropagation event)
    (rf/dispatch [:mouse-event {:element element
                                :target (.-target event)
                                :type (keyword (.-type event))
                                :mouse-pos [(.-clientX event) (.-clientY event)]
                                :button (.-buttons event)
                                :delta [(.-deltaX event) (.-deltaY event)]
                                :modifiers (cond-> #{}
                                             (.-altKey event) (conj :alt)
                                             (.-ctrlKey event) (conj :ctrl)
                                             (.-metaKey event) (conj :meta)
                                             (.-shiftKey event) (conj :shift))}])))
