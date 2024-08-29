(ns renderer.utils.element
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [reagent.dom.server :as dom.server]
   [renderer.element.db :as element.db]
   [renderer.snap.db :as snap.db]
   [renderer.tool.base :as tool]
   [renderer.utils.attribute :as attr]
   [renderer.utils.bcd :as bcd]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.map :as map]
   [renderer.utils.math :as math]))

(mx/defn root? :- boolean?
  [el :- element.db/element]
  (= :canvas (:tag el)))

(mx/defn svg? :- boolean?
  [el :- element.db/element]
  (= :svg (:tag el)))

(mx/defn container? :- boolean?
  [el :- element.db/element]
  (or (svg? el) (root? el)))

(mx/defn bounds :- [:maybe bounds/bounds]
  [elements]
  (let [el-bounds (->> elements (map :bounds) (remove nil?))]
    (when (seq el-bounds)
      (apply bounds/union el-bounds))))

(mx/defn offset :- math/vec2d
  [el :- element.db/element]
  (let [el-bounds (:bounds el)
        local-bounds (tool/bounds el)]
    (vec (take 2 (mat/sub el-bounds local-bounds)))))

(mx/defn snapping-points :- [:* math/vec2d]
  [element :- element.db/element, options :- [:set snap.db/options]]
  (let [points (or (tool/snapping-points element) [])]
    (if-let [bounds (:bounds element)]
      (let [[x1 y1 x2 y2] bounds
            [cx cy] (bounds/center bounds)]
        (cond-> points
          (:corners options)
          (into [[x1 y1]
                 [x1 y2]
                 [x2 y1]
                 [x2 y2]])

          (:centers options)
          (into [[cx cy]])

          (:midpoints options)
          (into [[x1 cy]
                 [x2 cy]
                 [cx y1]
                 [cx y2]])))
      points)))

(mx/defn attrs-map :- element.db/attrs
  [attrs]
  (let [deprecated-path [:__compat :status :deprecated]
        filtered-attrs (->> attrs
                            (filter #(not (get-in (val %) deprecated-path)))
                            (into {}))]
    (-> filtered-attrs
        (dissoc :__compat :systemLanguage)
        (keys)
        (zipmap (repeat "")))))

(mx/defn attributes :- element.db/attrs
  [{:keys [tag attrs]} :- element.db/element]
  (merge
   (when tag
     (merge (when (isa? tag ::tool/element)
              (merge (attrs-map (tag (:elements bcd/svg)))
                     (zipmap attr/core (repeat ""))))
            (when (contains? #{:animateMotion :animateTransform} tag)
              (attrs-map (:animate (:elements bcd/svg))))
            (zipmap (:attrs (tool/properties tag)) (repeat ""))))
   attrs))

(mx/defn supported-attr? :- boolean?
  [el :- element.db/element, k :- keyword?]
  (-> el attributes k boolean))

(mx/defn ->path  :- element.db/element
  [el :- element.db/element]
  (cond-> el
    (get-method tool/path (:tag el))
    (-> (assoc :attrs (attributes
                       {:tag :path
                        :attrs (map/merge-common-with
                                str
                                (:attrs el)
                                (attributes {:tag :path
                                             :attrs {}}))})
               :tag :path)
        (assoc-in [:attrs :d] (tool/path el)))))

(mx/defn stroke->path :- element.db/element
  [{:keys [attrs] :as el} :- element.db/element]
  (let [d (tool/path el)
        paper-path (Path. d)
        el-offset (or (:stroke-width attrs) 1)
        stroke-path (PaperOffset.offsetStroke
                     paper-path
                     (/ el-offset 2)
                     #js {:cap (or (:stroke-linecap attrs) "butt")
                          :join (or (:stroke-linejoin attrs) "miter")})
        new-d (.getAttribute (.exportSVG stroke-path) "d")]
    (-> el
        (assoc :attrs (attributes {:tag :path
                                   :attrs (map/merge-common-with
                                           str
                                           (dissoc (:attrs el) :stroke :stroke-width)
                                           (attributes {:tag :path
                                                        :attrs {}}))})
               :tag :path)
        (assoc-in [:attrs :d] new-d)
        (assoc-in [:attrs :fill] (:stroke attrs)))))

(mx/defn wrap-to-svg :- string?
  [s :- string?, [w h] :- math/vec2d]
  (str "<svg width='" w "' height='" h "'>" s "</svg>"))

(mx/defn ->string :- string?
  [els]
  (reduce #(-> (tool/render-to-string %2)
               (dom.server/render-to-static-markup)
               (str "\n" %)) "" els))

(mx/defn ->svg :- string?
  [els]
  (let [dimensions (bounds/->dimensions (bounds els))
        s (->string els)]
    (cond-> s
      (not (and (seq els)
                (empty? (rest els))
                (svg? (first els))))
      (wrap-to-svg dimensions))))
