(ns renderer.tree.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-fx
 ::focus
 (fn [[k direction]]
   (let [list-elements (.from js/Array (.querySelectorAll js/document ".tree-sidebar .list-item-button"))
         current-el (first (.querySelectorAll js/document (str ".tree-sidebar [data-id='" (name k) "']")))
         i (.indexOf list-elements current-el)
         element (get list-elements (case direction
                                      :up (if (zero? i) (dec (count list-elements)) (dec i))
                                      :down (if (< i (dec (count list-elements))) (inc i) 0)))]
     (.focus element))))

(rf/reg-event-fx
 ::focus-up
 (fn [_ [_ k]]
   {::focus [k :up]}))

(rf/reg-event-fx
 ::focus-up
 (fn [_ [_ k]]
   {::focus [k :down]}))
