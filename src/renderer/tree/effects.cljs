(ns renderer.tree.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]))

(defn query-by-id!
  [id]
  (.querySelectorAll js/document (str "#tree-sidebar [data-id='" id "']")))

(defn get-list-elements!
  []
  (.from js/Array (.querySelectorAll js/document "#tree-sidebar .list-item-button")))

(rf/reg-fx
 ::focus-next
 (fn [[id direction]]
   (let [list-elements (get-list-elements!)
         current-el (first (query-by-id! id))
         i (.indexOf list-elements current-el)
         max-i (dec (count list-elements))
         updated-i (case direction
                     :up (if (zero? i) max-i (dec i))
                     :down (if (< i max-i) (inc i) 0))
         element (get list-elements updated-i)]
     (.focus element))))

(rf/reg-fx
 ::select-range
 (fn [[last-focused-id id]]
   (let [list-elements (get-list-elements!)
         clicked-el (first (query-by-id! id))
         last-focus-el (first (query-by-id! last-focused-id))
         clicked-index (.indexOf list-elements clicked-el)
         focused-index (.indexOf list-elements last-focus-el)]
     (when-not (neg? focused-index)
       (let [index-range (apply range (if (< clicked-index focused-index)
                                        [clicked-index (inc focused-index)]
                                        [focused-index (inc clicked-index)]))
             ids (mapv #(-> (get list-elements %)
                            (.getAttribute "data-id")
                            (uuid)) index-range)]
         (rf/dispatch [::element.events/select-ids ids]))))))
