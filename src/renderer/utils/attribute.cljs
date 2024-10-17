(ns renderer.utils.attribute
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.string :as str]
   [malli.experimental :as mx]
   [renderer.element.db :as element.db :refer [Attrs Tag]]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.utils.bcd :as bcd]))

(def core
  #{:id :class :style})

(def presentation
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

(def order
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

(defn str->seq
  [s]
  (-> s str/trim (str/split #"\s*[\s,]\s*")))

(mx/defn points->vec :- vector?
  ([points :- string?]
   (points->vec points 2))
  ([points :- string?, step :- number?]
   (into [] (partition-all step) (str->seq points))))

(def camelcased
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
   "fontWeight"
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

(def lowercased
  (mapv str/lower-case camelcased))

(mx/defn ->camel-case :- keyword?
  [k :- keyword?]
  (let [i (->> k name str/lower-case (.indexOf lowercased))]
    (-> (if (= i -1) k (get camelcased i))
        (csk/->camelCaseString)
        (keyword))))

(def ->camel-case-memo (memoize ->camel-case))

(mx/defn ->map :- Attrs
  [attrs]
  (let [deprecated-path [:__compat :status :deprecated]
        filtered-attrs (->> attrs
                            (filter #(not (get-in (val %) deprecated-path)))
                            (into {}))]
    (-> filtered-attrs
        (dissoc :__compat :systemLanguage)
        (keys)
        (zipmap (repeat "")))))

(mx/defn defaults :- Attrs
  [tag :- Tag]
  (merge (when (element.db/tag? tag)
           (merge (->map (or (tag (:elements  bcd/svg)) {}))
                  (zipmap core (repeat ""))))
         (when (contains? #{:animateMotion :animateTransform} tag)
           (->map (:animate (:elements bcd/svg))))
         (zipmap (:attrs (element.hierarchy/properties tag)) (repeat ""))))

(def defaults-memo (memoize defaults))
