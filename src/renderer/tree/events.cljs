(ns renderer.tree.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-fx
 ::focus
 (fn [[k direction]]
   (let [list-elements (.from js/Array (.querySelectorAll js/document ".tree-sidebar .list-item-button"))
         current-el (first (.querySelectorAll js/document (str ".tree-sidebar [data-id='" (name k) "']")))
         i (.indexOf list-elements current-el)
         max-i (dec (count list-elements))
         updated-i (case direction
                     :up (if (zero? i) max-i (dec i))
                     :down (if (< i max-i) (inc i) 0))
         element (get list-elements updated-i)]
     (.focus element))))

(rf/reg-event-fx
 ::focus-up
 (fn [_ [_ k]]
   {::focus [k :up]}))

(rf/reg-event-fx
 ::focus-down
 (fn [_ [_ k]]
   {::focus [k :down]}))
