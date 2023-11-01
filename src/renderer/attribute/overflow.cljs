(ns renderer.attribute.overflow
  (:require
   [renderer.attribute.views :as views]
   [renderer.attribute.hierarchy :as hierarchy]))

(defmethod hierarchy/description :overflow
  []
  "The overflow attribute sets what to do when an element's content is too big 
   to fit in its block formatting context. This feature is not widely 
   implemented yet.")

(defmethod hierarchy/form-element :overflow
  [key value disabled? _initial]
  [views/select-input {:key key
                       :value value
                       :disabled? disabled?
                       ;; Although the initial value for overflow is auto, 
                       ;; it is overwritten in the User Agent style sheet 
                       ;; for the <svg> element when it is not the root element 
                       ;; of a stand-alone document, the <pattern> element, 
                       ;; and the <marker> element to be hidden by default.
                       :initial "hidden"
                       :default-value "hidden"
                       :items [{:key :visible
                                :value "visible"
                                :label "Visible"}
                               {:key :hidden
                                :value "hidden"
                                :label "Hidden"}
                               #_{:key :scroll
                                  :value "scroll"
                                  :label "Scroll"}
                               #_{:key :auto
                                  :value "auto"
                                  :label "Auto"}]}])