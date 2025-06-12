(ns renderer.attribute.impl.font-size
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/font-size"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.utils.i18n :refer [t]]))

(defmethod attribute.hierarchy/description [:default :font-size]
  []
  (t [::description 
      "The font-size attribute refers to the size of the font from baseline to
       baseline when multiple lines of text are set solid in a multiline layout environment."]))

(defmethod attribute.hierarchy/update-attr :font-size
  [el attribute f & more]
  (let [font-size (js/parseFloat (or (-> el :attrs attribute) 16))]
    (assoc-in el [:attrs attribute] (str (apply f font-size more)))))
