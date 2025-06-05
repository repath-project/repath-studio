(ns renderer.attribute.impl.font-weight
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/font-weight"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.subs :as-alias element.subs]))

(defmethod attribute.hierarchy/description [:default :font-weight]
  []
  "The font-weight attribute refers to the boldness or lightness of the glyphs
   used to render the text, relative to other fonts in the same font family.")

(def weight-name-mapping
  "https://developer.mozilla.org/en-US/docs/Web/CSS/@font-face/font-weight#common_weight_name_mapping"
  {"100" "Thin"
   "200" "ExtraLight"
   "300" "Light"
   "400" "Normal"
   "500" "Medium"
   "600" "SemiBold"
   "700" "Bold"
   "800" "ExtraBold"
   "900" "Black"})

(defmethod attribute.hierarchy/form-element [:default :font-weight]
  [_ k v attrs]
  (let [weights @(rf/subscribe [::element.subs/font-weights])
        weights (if (seq weights) weights (sort (keys weight-name-mapping)))]
    [attribute.views/select-input k v
     (merge attrs
            {:default-value "400"
             :items (mapv #(do {:key %
                                :label (str % " - " (-> % weight-name-mapping))
                                :value %}) weights)})]))
