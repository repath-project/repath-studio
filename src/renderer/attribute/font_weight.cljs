(ns renderer.attribute.font-weight
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/font-weight"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.element.subs :as-alias element.s]))

(defmethod hierarchy/description [:default :font-weight]
  []
  "The font-weight attribute refers to the boldness or lightness of the glyphs
   used to render the text, relative to other fonts in the same font family.")

(def name-mapping
  "https://developer.mozilla.org/en-US/docs/Web/CSS/@font-face/font-weight#common_weight_name_mapping"
  {100 "Thin (Hairline)"
   200 "Extra Light (Ultra Light)"
   300 "Light"
   400 "Normal"
   500 "Medium"
   600 "Semi Bold (Demi Bold)"
   700 "Bold"
   800 "Extra Bold (Ultra Bold)"
   900 "Black (Heavy)"})

(defmethod hierarchy/form-element [:default :font-weight]
  [_ k v attrs]
  (let [weights @(rf/subscribe [::element.s/font-weights])
        weights (if (seq weights) weights (sort (keys name-mapping)))]
    [v/select-input k v (merge attrs
                               {:default-value "400"
                                :items (mapv #(do {:key %
                                                   :label (str % " - " (-> % name-mapping))
                                                   :value (str %)}) weights)})]))
