(ns renderer.element.views
  (:require
   [renderer.element.events :as-alias element.events]))

(def context-menu
  ;; TODO: Add and group actions
  [{:label "Cut"
    :action [::element.events/cut]}
   {:label "Copy"
    :action [::element.events/copy]}
   {:label "Paste"
    :action [::element.events/paste]}
   {:type :separator}
   {:label "Raise"
    :action [::element.events/raise]}
   {:label "Lower"
    :action [::element.events/lower]}
   {:label "Raise to top"
    :action [::element.events/raise-to-top]}
   {:label "Lower to bottom"
    :action [::element.events/lower-to-bottom]}
   {:type :separator}
   {:label "Animate"
    :action [::element.events/animate :animate {}]}
   {:label "Animate Transform"
    :action [::element.events/animate :animateTransform {}]}
   {:label "Animate Motion"
    :action [::element.events/animate :animateMotion {}]}
   {:type :separator}
   {:label "Duplicate"
    :action [::element.events/duplicate]}
   {:label "Delete"
    :action [::element.events/delete]}])
