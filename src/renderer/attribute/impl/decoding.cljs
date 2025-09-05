(ns renderer.attribute.impl.decoding
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/decoding"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.utils.i18n :refer [t]]))

(defmethod attribute.hierarchy/initial [:default :decoding] [] "auto")

(defmethod attribute.hierarchy/description [:default :decoding]
  []
  (t [::description
      "The decoding attribute, valid on <image> elements, provides a hint to the browser
       as to whether it should perform image decoding along with rendering other content
       in a single presentation step that looks more 'correct' (sync), or render and
       present the other content first and then decode the image and present it later
       (async). In practice, async means that the next paint does not wait for the image
       to decode."]))

(defmethod attribute.hierarchy/form-element [:default :decoding]
  [_ k v attrs]
  [attribute.views/select-input k v
   (merge attrs {:items [{:key :sync
                          :value "sync"
                          :label (t [::sync "Synchronously"])}
                         {:key :async
                          :value "async"
                          :label (t [::async "Asynchronously"])}
                         {:key :auto
                          :value "auto"
                          :label (t [::auto "Auto"])}]})])
