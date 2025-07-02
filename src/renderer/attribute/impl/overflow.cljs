(ns renderer.attribute.impl.overflow
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/overflow"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.utils.i18n :refer [t]]))

(defmethod attribute.hierarchy/description [:default :overflow]
  []
  (t [::description 
      "The overflow attribute sets what to do when an element's content is too big
       to fit in its block formatting context. This feature is not widely
       implemented yet."]))

(defmethod attribute.hierarchy/form-element [:default :overflow]
  [_ k v {:keys [disabled]}]
  [attribute.views/select-input k v
   {:disabled disabled
    ;; Although the initial value for overflow is auto, it is overwritten
    ;; in the User Agent style sheet for the <svg> element when it is not
    ;; the root element of a stand-alone document, the <pattern> element,
    ;; and the <marker> element to be hidden by default.
    :placeholder "hidden"
    :default-value "hidden"
    :items [{:key :visible
             :value "visible"
             :label (t [::visible "Visible"])}
            {:key :hidden
             :value "hidden"
             :label (t [::hidden "Hidden"])}
            #_{:key :scroll
               :value "scroll"
               :label "Scroll"}
            #_{:key :auto
               :value "auto"
               :label "Auto"}]}])
