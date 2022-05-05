(ns repath.studio.mouse
  (:require
   [re-frame.core :as rf]))

(defn multiselect?
  [event]
  (some #(contains? (:modifiers event) %) #{:ctrl :shift}))

(defn event-handler
  [event element]
  (when (and event (not= (.-buttons event) 2)) ; Exclude right click that should be used for the contect menu exclusively.
    (.stopPropagation event)
    (rf/dispatch [:mouse-event {:element element
                                :target (.-target event)
                                :type (keyword (.-type event))
                                :mouse-pos [(.-clientX event) (.-clientY event)]
                                :button (.-button event)
                                :buttons (.-button event)
                                :delta [(.-deltaX event) (.-deltaY event)]
                                :modifiers (cond-> #{}
                                             (.-altKey event) (conj :alt)
                                             (.-ctrlKey event) (conj :ctrl)
                                             (.-metaKey event) (conj :meta)
                                             (.-shiftKey event) (conj :shift))}])))
