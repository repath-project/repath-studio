(ns renderer.element.views
  (:require
   [renderer.element.events :as-alias element.e]))


(def context-menu
  ;; TODO: Add and group actions
  [{:label "Cut"
    :action [::element.e/cut]}
   {:label "Copy"
    :action [::element.e/copy]}
   {:label "Paste"
    :action [::element.e/paste]}
   {:type :separator}
   {:label "Raise"
    :action [::element.e/raise]}
   {:label "Lower"
    :action [::element.e/lower]}
   {:label "Raise to top"
    :action [::element.e/raise-to-top]}
   {:label "Lower to bottom"
    :action [::element.e/lower-to-bottom]}
   {:type :separator}
   {:label "Animate"
    :action [::element.e/animate :animate {}]}
   {:label "Animate Transform"
    :action [::element.e/animate :animateTransform {}]}
   {:label "Animate Motion"
    :action [::element.e/animate :animateMotion {}]}
   {:type :separator}
   {:label "Duplicate in position"
    :action [::element.e/duplicate-in-place]}
   {:label "Delete"
    :action [::element.e/delete]}])
