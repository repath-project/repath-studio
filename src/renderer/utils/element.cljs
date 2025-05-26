(ns renderer.utils.element
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   ["style-to-object" :default parse]
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [reagent.dom.server :as dom.server]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.db :refer [Element Attrs]]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.snap.db :refer [SnapOptions]]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.bounds :as utils.bounds :refer [BBox]]
   [renderer.utils.map :as utils.map]
   [renderer.utils.math :refer [Vec2]]))

(m/=> root? [:-> Element boolean?])
(defn root?
  [el]
  (= :canvas (:tag el)))

(m/=> svg? [:-> Element boolean?])
(defn svg?
  [el]
  (= :svg (:tag el)))

(m/=> container? [:-> Element boolean?])
(defn container?
  [el]
  (or (svg? el) (root? el)))

(m/=> properties [:-> Element [:maybe map?]])
(defn properties
  [el]
  (-> el :tag element.hierarchy/properties))

(m/=> ratio-locked? [:-> Element boolean?])
(defn ratio-locked?
  [el]
  (-> el properties :ratio-locked boolean))

(m/=> united-bbox [:-> [:sequential Element] [:maybe BBox]])
(defn united-bbox
  [elements]
  (let [el-bbox (keep :bbox elements)]
    (when (seq el-bbox)
      (apply utils.bounds/union el-bbox))))

(m/=> offset [:-> Element Vec2])
(defn offset
  [el]
  (let [el-bbox (:bbox el)
        local-bbox (element.hierarchy/bbox el)]
    (->> (matrix/sub el-bbox local-bbox)
         (take 2)
         (vec))))

(m/=> snapping-points [:-> Element SnapOptions [:* Vec2]])
(defn snapping-points
  [el options]
  (let [points (or (when (contains? options :nodes)
                     (mapv #(with-meta
                              (matrix/add % (offset el))
                              (merge (meta %) {:id (:id el)}))
                           (element.hierarchy/snapping-points el))) [])]
    (cond-> points
      (:bbox el)
      (into (mapv #(with-meta % (merge (meta %) {:id (:id el)}))
                  (utils.bounds/->snapping-points (:bbox el) options))))))

(m/=> attributes [:-> Element Attrs])
(defn attributes
  "Returns existing attributes merged with defaults."
  [{:keys [tag attrs]}]
  (cond->> attrs
    tag
    (merge (utils.attribute/defaults-memo tag))))

(m/=> normalize-attrs [:-> map? Element])
(defn normalize-attrs
  [el]
  (-> el
      (update :attrs update-keys utils.attribute/->camel-case-memo)
      (update :attrs update-vals str)))

(m/=> supported-attr? [:-> Element keyword? boolean?])
(defn supported-attr?
  [el k]
  (-> el attributes k boolean))

(m/=> ->path [:-> Element Element])
(defn ->path
  [el]
  (cond-> el
    (get-method element.hierarchy/path (:tag el))
    (-> (assoc :tag :path)
        (update :attrs #(utils.map/merge-common-with str % (utils.attribute/defaults-memo :path)))
        (assoc-in [:attrs :d] (element.hierarchy/path el)))))

(m/=> stroke->path [:-> Element Element])
(defn stroke->path
  [{:keys [attrs] :as el}]
  (let [d (element.hierarchy/path el)
        paper-path (Path. d)
        el-offset (or (:stroke-width attrs) 1)
        stroke-path (PaperOffset.offsetStroke
                     paper-path
                     (/ el-offset 2)
                     #js {:cap (or (:stroke-linecap attrs) "butt")
                          :join (or (:stroke-linejoin attrs) "miter")})
        new-d (.getAttribute (.exportSVG stroke-path) "d")]
    (-> (assoc el :tag :path)
        (update :attrs dissoc :stroke :stroke-width)
        (update :attrs #(utils.map/merge-common-with str % (utils.attribute/defaults-memo :path)))
        (assoc-in [:attrs :d] new-d)
        (assoc-in [:attrs :fill] (:stroke attrs)))))

(m/=> wrap-to-svg [:-> string? Vec2 string?])
(defn wrap-to-svg
  [s [w h]]
  (str "<svg width='" w "' height='" h "'>" s "</svg>"))

(m/=> ->string [:-> [:sequential Element] string?])
(defn ->string
  [els]
  (reduce #(-> (element.hierarchy/render-to-string %2)
               (dom.server/render-to-static-markup)
               (str "\n" %)) "" els))

(m/=> ->svg [:-> [:sequential Element] string?])
(defn ->svg
  [els]
  (cond-> (->string els)
    (not (and (seq els)
              (empty? (rest els))
              (svg? (first els))))
    (wrap-to-svg (utils.bounds/->dimensions (united-bbox els)))))

(m/=> style->map [:-> Attrs Attrs])
(defn style->map
  "Converts :style attribute to map.
   Parsing might through an exception. When that happens, we remove the attribute
   because there is no other way to handle this gracefully."
  [attrs]
  (try (cond-> (update attrs :style parse)
         (nil? (:style attrs))
         (dissoc :style))
       (catch :default _e (dissoc attrs :style))))

(m/=> update-attrs-with [:-> Element ifn? [:vector vector?] Element])
(defn update-attrs-with
  [el f attrs-map]
  (reduce (fn [el [k & more]]
            (apply attribute.hierarchy/update-attr el k f more)) el attrs-map))

(m/=> scale-offset [:-> Vec2 Vec2 Vec2])
(defn scale-offset
  [ratio pivot-point]
  (->> ratio
       (matrix/mul pivot-point)
       (matrix/sub pivot-point)))
