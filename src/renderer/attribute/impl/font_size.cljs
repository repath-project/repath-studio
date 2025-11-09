(ns renderer.attribute.impl.font-size
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/font-size"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.utils.font :as utils.font]
   [renderer.utils.length :as utils.length]))

(defmethod attribute.hierarchy/description [:default :font-size]
  []
  [::description
   "The font-size attribute refers to the size of the font from baseline to
    baseline when multiple lines of text are set solid in a multiline layout
    environment."])

(defmethod attribute.hierarchy/update-attr :font-size
  [el attribute f & more]
  (let [font-size (:font-size (utils.font/get-computed-styles! el))
        font-size (utils.length/unit->px font-size)]
    (assoc-in el [:attrs attribute] (str (apply f font-size more)))))

(def sizes
  ["xx-small"
   "x-small"
   "small"
   "medium"
   "large"
   "x-large"
   "xx-large"
   "xxx-large"])

(defmethod attribute.hierarchy/form-element [:default :font-size]
  [_ k v attrs]
  [attribute.views/select-input k v
   (merge attrs
          {:default-value "medium"
           :items (mapv #(do {:key %
                              :label %
                              :value %}) sizes)})])
