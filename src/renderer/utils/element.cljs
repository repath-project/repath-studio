(ns renderer.utils.element
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   ["style-to-object" :default parse]
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [reagent.dom.server :as dom.server]
   [renderer.element.db :refer [Element Attrs]]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.snap.db :refer [SnapOptions]]
   [renderer.utils.attribute :as attr]
   [renderer.utils.bounds :as utils.bounds :refer [Bounds]]
   [renderer.utils.map :as map]
   [renderer.utils.math :refer [Vec2D]]))

(mx/defn root? :- boolean?
  [el :- Element]
  (= :canvas (:tag el)))

(mx/defn svg? :- boolean?
  [el :- Element]
  (= :svg (:tag el)))

(mx/defn container? :- boolean?
  [el :- Element]
  (or (svg? el) (root? el)))

(mx/defn properties :- [:maybe map?]
  [el :- Element]
  (-> el :tag element.hierarchy/properties))

(mx/defn ratio-locked? :- [:maybe boolean?]
  [el :- Element]
  (-> el properties :ratio-locked))

(mx/defn united-bounds :- [:maybe Bounds]
  [elements :- [:sequential Element]]
  (let [el-bounds (->> elements (map :bounds) (remove nil?))]
    (when (seq el-bounds)
      (apply utils.bounds/union el-bounds))))

(mx/defn offset :- Vec2D
  [el :- Element]
  (let [el-bounds (:bounds el)
        local-bounds (element.hierarchy/bounds el)]
    (vec (take 2 (mat/sub el-bounds local-bounds)))))

(mx/defn snapping-points :- [:* Vec2D]
  [el :- Element, options :- SnapOptions]
  (let [points (or (element.hierarchy/snapping-points el) [])]
    (if-let [bounds (:bounds el)]
      (let [[x1 y1 x2 y2] bounds
            [cx cy] (utils.bounds/center bounds)]
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

(mx/defn attributes :- Attrs
  "Returns existing attributes merged with defaults."
  [{:keys [tag attrs]} :- Element]
  (cond->> attrs
    tag
    (merge (attr/defaults-memo tag))))

(mx/defn normalize-attrs :- Element
  [el :- Element]
  (update el :attrs #(update-keys % attr/->camel-case-memo)))

(mx/defn supported-attr? :- boolean?
  [el :- Element, k :- keyword?]
  (-> el attributes k boolean))

(mx/defn ->path  :- Element
  [el :- Element]
  (cond-> el
    (get-method element.hierarchy/path (:tag el))
    (-> (assoc :tag :path)
        (update :attrs #(map/merge-common-with str % (attr/defaults-memo :path)))
        (assoc-in [:attrs :d] (element.hierarchy/path el)))))

(mx/defn stroke->path :- Element
  [{:keys [attrs] :as el} :- Element]
  (let [d (element.hierarchy/path el)
        paper-path (Path. d)
        el-offset (or (:stroke-width attrs) 1)
        stroke-path (PaperOffset.offsetStroke
                     paper-path
                     (/ el-offset 2)
                     #js {:cap (or (:stroke-linecap attrs) "butt")
                          :join (or (:stroke-linejoin attrs) "miter")})
        new-d (.getAttribute (.exportSVG stroke-path) "d")]
    (-> el
        (assoc :tag :path)
        (update :attrs dissoc :stroke :stroke-width)
        (update :attrs #(map/merge-common-with str % (attr/defaults-memo :path)))
        (assoc-in [:attrs :d] new-d)
        (assoc-in [:attrs :fill] (:stroke attrs)))))

(mx/defn wrap-to-svg :- string?
  [s :- string?, [w h] :- Vec2D]
  (str "<svg width='" w "' height='" h "'>" s "</svg>"))

(mx/defn ->string :- string?
  [els]
  (reduce #(-> (element.hierarchy/render-to-string %2)
               (dom.server/render-to-static-markup)
               (str "\n" %)) "" els))

(mx/defn ->svg :- string?
  [els]
  (cond-> (->string els)
    (not (and (seq els)
              (empty? (rest els))
              (svg? (first els))))
    (wrap-to-svg (utils.bounds/->dimensions (united-bounds els)))))

(mx/defn style->map
  "Conversts :style attribute to map.
   Parsing might through an exception. When that hapens, we remove the attribute
   because there is no other way to handle this gracefully."
  [attrs :- Attrs]
  (try (update attrs :style parse)
       (catch :default _e (dissoc attrs :style))))
