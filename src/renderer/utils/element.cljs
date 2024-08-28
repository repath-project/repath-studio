(ns renderer.utils.element
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   [clojure.core.matrix :as mat]
   [renderer.tool.base :as tool]
   [renderer.utils.attribute :as attr]
   [renderer.utils.bcd :as bcd]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.map :as map]))

(defn root?
  [el]
  (= :canvas (:tag el)))

(defn svg?
  [el]
  (= :svg (:tag el)))

(defn container?
  [el]
  #_(isa? (:tag el) ::tool/container)
  (or (svg? el) (root? el))) ; FIXME

(defn bounds
  [elements]
  (let [el-bounds (->> elements (map :bounds) (remove nil?))]
    (when (seq el-bounds)
      (apply bounds/union el-bounds))))

(defn offset
  [el]
  (let [el-bounds (:bounds el)
        local-bounds (tool/bounds el)]
    (take 2 (mat/sub el-bounds local-bounds))))

(defn snapping-points
  [element options]
  (when-let [bounds (:bounds element)]
    (let [[x1 y1 x2 y2] bounds
          [cx cy] (bounds/center bounds)]
      (concat (when (:corners options)
                [[x1 y1]
                 [x1 y2]
                 [x2 y1]
                 [x2 y2]])

              (when (:centers options)
                [[cx cy]])

              (when (:midpoints options)
                [[x1 cy]
                 [x2 cy]
                 [cx y1]
                 [cx y2]])

              (when :nodes
                (tool/snapping-points element))))))

(defn- attrs-map
  [attrs]
  (let [deprecated-path [:__compat :status :deprecated]
        filtered-attrs (->> attrs
                            (filter #(not (get-in (val %) deprecated-path)))
                            (into {}))]
    (-> filtered-attrs
        (dissoc :__compat :systemLanguage)
        keys
        (zipmap (repeat "")))))

(defn attributes
  [{:keys [tag attrs]}]
  (merge
   (when tag
     (merge (when (isa? tag ::tool/element)
              (merge (attrs-map (tag (:elements bcd/svg)))
                     (zipmap attr/core (repeat ""))))
            (when (contains? #{:animateMotion :animateTransform} tag)
              (attrs-map (:animate (:elements bcd/svg))))
            (zipmap (:attrs (tool/properties tag)) (repeat ""))))
   attrs))

(defn supported-attr?
  [el k]
  (-> el attributes k))

(defn ->path
  [el]
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

(defn stroke->path
  [{:keys [attrs] :as el}]
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

(defn wrap-to-svg
  [s [w h]]
  (str "<svg width='" w "' height='" h "'>" s "</svg>"))
