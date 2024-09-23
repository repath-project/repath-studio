(ns renderer.utils.element
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [reagent.dom.server :as dom.server]
   [renderer.element.db :refer [Element Attr]]
   [renderer.snap.db :refer [SnapOption]]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.attribute :as attr]
   [renderer.utils.bcd :as bcd]
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

(mx/defn united-bounds :- [:maybe Bounds]
  [elements :- [:sequential Element]]
  (let [el-bounds (->> elements (map :bounds) (remove nil?))]
    (when (seq el-bounds)
      (apply utils.bounds/union el-bounds))))

(mx/defn offset :- Vec2D
  [el :- Element]
  (let [el-bounds (:bounds el)
        local-bounds (tool.hierarchy/bounds el)]
    (vec (take 2 (mat/sub el-bounds local-bounds)))))

(mx/defn snapping-points :- [:* Vec2D]
  [el :- Element, options :- [:set SnapOption]]
  (let [points (or (tool.hierarchy/snapping-points el) [])]
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

(mx/defn attrs-map :- [:map-of keyword? Attr]
  [attrs]
  (let [deprecated-path [:__compat :status :deprecated]
        filtered-attrs (->> attrs
                            (filter #(not (get-in (val %) deprecated-path)))
                            (into {}))]
    (-> filtered-attrs
        (dissoc :__compat :systemLanguage)
        (keys)
        (zipmap (repeat "")))))

(mx/defn attributes :- [:map-of keyword? Attr]
  [{:keys [tag attrs]} :- Element]
  (merge
   (when tag
     (merge (when (isa? tag ::tool.hierarchy/element)
              (merge (attrs-map (tag (:elements bcd/svg)))
                     (zipmap attr/core (repeat ""))))
            (when (contains? #{:animateMotion :animateTransform} tag)
              (attrs-map (:animate (:elements bcd/svg))))
            (zipmap (:attrs (tool.hierarchy/properties tag)) (repeat ""))))
   attrs))

(mx/defn supported-attr? :- boolean?
  [el :- Element, k :- keyword?]
  (-> el attributes k boolean))

(mx/defn ->path  :- Element
  [el :- Element]
  (cond-> el
    (get-method tool.hierarchy/path (:tag el))
    (-> (assoc :attrs (attributes
                       {:tag :path
                        :attrs (map/merge-common-with
                                str
                                (:attrs el)
                                (attributes {:tag :path
                                             :attrs {}}))})
               :tag :path)
        (assoc-in [:attrs :d] (tool.hierarchy/path el)))))

(mx/defn stroke->path :- Element
  [{:keys [attrs] :as el} :- Element]
  (let [d (tool.hierarchy/path el)
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
  [s :- string?, [w h] :- Vec2D]
  (str "<svg width='" w "' height='" h "'>" s "</svg>"))

(mx/defn ->string :- string?
  [els]
  (reduce #(-> (tool.hierarchy/render-to-string %2)
               (dom.server/render-to-static-markup)
               (str "\n" %)) "" els))

(mx/defn ->svg :- string?
  [els]
  (cond-> (->string els)
    (not (and (seq els)
              (empty? (rest els))
              (svg? (first els))))
    (wrap-to-svg (utils.bounds/->dimensions (united-bounds els)))))
