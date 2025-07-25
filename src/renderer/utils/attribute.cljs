(ns renderer.utils.attribute
  (:require
   ["@mdn/browser-compat-data" :as bcd]
   ["mdn-data" :as mdn]
   [camel-snake-kebab.core :as camel-snake-kebab]
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.db :as element.db :refer [Attrs Tag]]
   [renderer.element.hierarchy :as element.hierarchy]))

;; https://github.com/mdn/data/blob/main/docs/updating_css_json.md
(defonce mdn-data (js->clj mdn :keywordize-keys true))

;; https://github.com/mdn/browser-compat-data
(defonce svg-data (js->clj (.-svg bcd) :keywordize-keys true))

(defonce core  #{:id :class :style})

(defonce presentation
  #{:text-anchor :text-rendering :font-style :mask :image-rendering
    :stroke-dasharray :fill-rule :font-stretch :text-overflow :vector-effect
    :stroke :stop-color :clip :glyph-orientation-horizontal :solid-opacity
    :transform :color :white-space :font-size :kerning :font-variant
    :writing-mode :font-weight :overflow :clip-rule :stroke-opacity :fill
    :color-profile :stroke-linejoin :shape-rendering :cursor :stroke-dashoffset
    :word-spacing :clip-path :stroke-linecap :flood-opacity :lighting-color
    :alignment-baseline :dominant-baseline :marker-start :filter :stroke-width
    :opacity :baseline-shift :color-interpolation-filters :transform-origin
    :text-decoration :display :stroke-miterlimit :letter-spacing :flood-color
    :unicode-bidi :marker-mid :pointer-events :font-size-adjust
    :glyph-orientation-vertical :color-interpolation :visibility
    :enable-background :direction :fill-opacity :solid-color :font-family
    :marker-end :paint-order})

(defonce order
  [:href
   :d
   :points
   :x :y
   :x1 :y1
   :x2 :y2
   :cx :cy
   :dx :dy
   :width :height
   :rx :ry
   :r
   :rotate
   :transform
   :font-family :font-size :font-weight :font-style
   :textLength
   :lengthAdjust
   :viewBox :preserveAspectRatio
   :stroke
   :fill
   :stroke-width :stroke-linecap :stroke-linejoin :stroke-dasharray
   :crossorigin
   :decoding
   :opacity
   :overflow
   :id :class :tab-index
   :style])

;; https://developer.mozilla.org/en-US/docs/Web/CSS/@font-face/font-weight#common_weight_name_mapping
(defonce weight-name-mapping
  {"100" ["Thin" "Hairline"]
   "200" ["ExtraLight" "UltraLight"]
   "300" ["Light"]
   "400" ["Regular" "Normal" "Book"]
   "500" ["Medium"]
   "600" ["SemiBold" "DemiBold"]
   "700" ["Bold"]
   "800" ["ExtraBold" "UltraBold"]
   "900" ["Black" "Heavy"]})

(defonce camelcased
  ["accentHeight"
   "alignmentBaseline"
   "allowReorder"
   "arabicForm"
   "attributeName"
   "attributeType"
   "autoReverse"
   "baseFrequency"
   "baseProfile"
   "baselineShift"
   "calcMode"
   "capHeight"
   "clipPath"
   "clipPathUnits"
   "clipRule"
   "colorInterpolation"
   "colorInterpolationFilters"
   "colorProfile"
   "colorRendering"
   "contentScriptType"
   "contentStyleType"
   "diffuseConstant"
   "dominantBaseline"
   "edgeMode"
   "enableBackground"
   "externalResourcesRequired"
   "fillOpacity"
   "fillRule"
   "filterRes"
   "filterUnits"
   "floodColor"
   "floodOpacity"
   "fontFamily"
   "fontSize"
   "fontSizeAdjust"
   "fontStretch"
   "fontStyle"
   "fontVariant"
   "glyphName"
   "glyphOrientationHorizontal"
   "glyphOrientationVertical"
   "glyphRef"
   "gradientTransform"
   "gradientUnits"
   "horizAdvX"
   "horizOriginX"
   "imageRendering"
   "kernelMatrix"
   "kernelUnitLength"
   "keyPoints"
   "keySplines"
   "keyTimes"
   "lengthAdjust"
   "letterSpacing"
   "lightingColor"
   "limitingConeAngle"
   "markerEnd"
   "markerHeight"
   "markerMid"
   "markerStart"
   "markerUnits"
   "markerWidth"
   "maskContentUnits"
   "maskUnits"
   "mathematical"
   "numOctaves"
   "overlinePosition"
   "overlineThickness"
   "paintOrder"
   "pathLength"
   "patternContentUnits"
   "patternTransform"
   "patternUnits"
   "pointerEvents"
   "pointsAtX"
   "pointsAtY"
   "pointsAtZ"
   "preserveAlpha"
   "preserveAspectRatio"
   "primitiveUnits"
   "refX"
   "refY"
   "renderingIntent"
   "repeatCount"
   "repeatDur"
   "requiredExtensions"
   "requiredFeatures"
   "shapeRendering"
   "specularConstant"
   "specularExponent"
   "spreadMethod"
   "startOffset"
   "stdDeviation"
   "stitchTiles"
   "stopColor"
   "stopOpacity"
   "strikethroughPosition"
   "strikethroughThickness"
   "strokeDasharray"
   "strokeDashoffset"
   "strokeLinecap"
   "strokeLinejoin"
   "strokeMiterlimit"
   "strokeOpacity"
   "strokeWidth"
   "surfaceScale"
   "systemLanguage"
   "tableValues"
   "targetX"
   "targetY"
   "textAnchor"
   "textDecoration"
   "textLength"
   "textRendering"
   "underlinePosition"
   "underlineThickness"
   "unicodeBidi"
   "unicodeRange"
   "unitsPerEm"
   "vAlphabetic"
   "vHanging"
   "vIdeographic"
   "vMathematical"
   "vectorEffect"
   "vertAdvY"
   "vertOriginX"
   "vertOriginY"
   "viewBox"
   "viewTarget"
   "wordSpacing"
   "writingMode"
   "xChannelSelector"
   "xHeight"
   "xlinkActuate"
   "xlinkArcrole"
   "xlinkHref"
   "xlinkRole"
   "xlinkShow"
   "xlinkTitle"
   "xlinkType"
   "xmlnsXlink"
   "xmlBase"
   "xmlLang"
   "xmlSpace"
   "yChannelSelector"
   "zoomAndPan"])

(defonce lowercased (mapv string/lower-case camelcased))

(m/=> compatibility [:function
                     [:-> Tag map?]
                     [:-> Tag keyword? map?]])
(defn compatibility
  "Returns compatibility data for tags or attributes."
  ([tag]
   (-> svg-data :elements tag :__compat))
  ([tag attr]
   (or (-> svg-data :elements tag attr :__compat)
       (-> svg-data :global_attributes attr :__compat))))

(defn enhance-data-readability
  [property k]
  (cond-> property
    (and (get property k)
         (string? (get property k)))
    (update k #(-> (camel-snake-kebab/->kebab-case-string %)
                   (string/replace "-" " ")))))

(m/=> property-data [:-> keyword? any?])
(defn property-data
  [k]
  (let [css-property (get-in mdn-data [:css :properties k])]
    (reduce enhance-data-readability
            css-property
            [:appliesto :computed :percentages :animationType])))

(def property-data-memo (memoize property-data))

(defonce whitespace-regex #"\s*[\s,]\s*")

(m/=> str->seq [:-> string? vector?])
(defn str->seq
  [s]
  (-> s string/trim (string/split whitespace-regex)))

(m/=> points->vec [:function
                   [:-> string? vector?]
                   [:-> string? pos-int? vector?]])
(defn points->vec
  ([points]
   (points->vec points 2))
  ([points step]
   (into [] (partition-all step) (str->seq points))))

(m/=> ->camel-case [:-> keyword? keyword?])
(defn ->camel-case
  [k]
  (let [i (->> k name string/lower-case (.indexOf lowercased))]
    (-> (if (= i -1) k (get camelcased i))
        (camel-snake-kebab/->camelCaseString)
        (keyword))))

(def ->camel-case-memo (memoize ->camel-case))

(m/=> ->attrs [:-> map? Attrs])
(defn ->attrs
  [attrs]
  (let [deprecated-path [:__compat :status :deprecated]
        filtered-attrs (->> attrs
                            (filter #(not (get-in (val %) deprecated-path)))
                            (into {}))]
    (-> filtered-attrs
        (dissoc :__compat :systemLanguage)
        (keys)
        (zipmap (repeat "")))))

(def ->attrs-memo (memoize ->attrs))

(m/=> defaults [:-> Tag Attrs])
(defn defaults
  [tag]
  (merge (when (element.db/tag? tag)
           (merge (->attrs-memo (or (tag (:elements svg-data)) {}))
                  (when (or (isa? tag ::element.hierarchy/shape)
                            (isa? tag ::element.hierarchy/container))
                    (zipmap core (repeat "")))))
         (when (contains? #{:animateMotion :animateTransform} tag)
           (->attrs-memo (:animate (:elements svg-data))))
         (zipmap (:attrs (element.hierarchy/properties tag)) (repeat ""))))

(def defaults-memo (memoize defaults))

(m/=> initial [:-> Tag keyword? string?])
(defn initial
  [tag k]
  (let [dispatch-tag (if (contains? (methods attribute.hierarchy/initial) [tag k])
                       tag
                       :default)]
    (or (attribute.hierarchy/initial dispatch-tag k)
        (:initial (property-data-memo k)))))

(def initial-memo (memoize initial))

(m/=> defaults-with-vals [:-> Tag Attrs])
(defn defaults-with-vals
  [tag]
  (->> (defaults-memo tag)
       (map (fn [[k v]] [k (or (initial-memo tag k) v)]))
       (into {})))
