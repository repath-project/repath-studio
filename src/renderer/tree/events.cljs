(ns renderer.tree.events
  (:require
   [re-frame.core :as rf]
   [renderer.document.events :as-alias document.e]
   [renderer.element.events :as-alias element.e]))

(rf/reg-fx
 ::focus-item
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
 ::key-down
 (fn [_ [_ key el-k multi?]]
   (case key
     "ArrowUp"
     {::focus-item [el-k :up]}

     "ArrowDown"
     {::focus-item [el-k :down]}

     "ArrowLeft"
     {:dispatch [::document.e/collapse-el el-k]}

     "ArrowRight"
     {:dispatch [::document.e/expand-el el-k]}

     "Enter"
     {:dispatch [::element.e/select el-k multi?]}

     nil)))
