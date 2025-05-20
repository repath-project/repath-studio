(ns renderer.element.views
  (:require
   [renderer.element.events :as-alias element.events]
   [renderer.utils.i18n :refer [t]]))

(def context-menu
  ;; TODO: Add and group actions
  [{:label (t [::cut "Cut"])
    :action [::element.events/cut]}
   {:label (t [::copy "Copy"])
    :action [::element.events/copy]}
   {:label (t [::paste "Paste"])
    :action [::element.events/paste]}
   {:type :separator}
   {:label (t [::raise "Raise"])
    :action [::element.events/raise]}
   {:label (t [::lower "Lower"])
    :action [::element.events/lower]}
   {:label (t [::raise-top "Raise to top"])
    :action [::element.events/raise-to-top]}
   {:label (t [::lower-bottom "Lower to bottom"])
    :action [::element.events/lower-to-bottom]}
   {:type :separator}
   {:label (t [::animate "Animate"])
    :action [::element.events/animate :animate {}]}
   {:label (t [::animate-transform "Animate Transform"])
    :action [::element.events/animate :animateTransform {}]}
   {:label (t [::animate-motion "Animate Motion"])
    :action [::element.events/animate :animateMotion {}]}
   {:type :separator}
   {:label (t [::duplicate "Duplicate"])
    :action [::element.events/duplicate]}
   {:label (t [::delete "Delete"])
    :action [::element.events/delete]}])
