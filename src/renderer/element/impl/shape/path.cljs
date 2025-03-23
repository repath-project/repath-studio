(ns renderer.element.impl.shape.path
  "https://www.w3.org/TR/SVG/paths.html#PathElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/path"
  (:require
   ["svg-path-bbox" :refer [svgPathBbox]]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.tool.views :as tool.v]
   [renderer.utils.element :as element]
   [renderer.utils.length :as length]))

(derive :path ::hierarchy/shape)

(defmethod hierarchy/properties :path
  []
  {:icon "bezier-curve"
   :description "The <path> SVG element is the generic element to define a shape.
                 All the basic shapes can be created with a path element."
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linejoin
           :opacity]})

(defmethod hierarchy/translate :path
  [el [x y]]
  (update-in el [:attrs :d] #(-> (svgpath %)
                                 (.translate x y)
                                 (.toString))))

(defmethod hierarchy/scale :path
  [el ratio pivot-point]
  (let [[scale-x scale-y] ratio
        [x y] (hierarchy/bbox el)
        [x y] (mat/sub (mat/add [x y]
                                (mat/sub pivot-point
                                         (mat/mul pivot-point ratio)))
                       (mat/mul ratio [x y]))]
    (update-in el [:attrs :d] #(-> (svgpath %)
                                   (.scale scale-x scale-y)
                                   (.translate x y)
                                   (.toString)))))

(defmethod hierarchy/bbox :path
  [{{:keys [d]} :attrs}]
  (let [[left top right bottom] (js->clj (svgPathBbox d))]
    [left top right bottom]))

(defmethod hierarchy/render-edit :path
  [el]
  (let [offset (element/offset el)
        segments (-> el :attrs :d svgpath .-segments)
        square-handle (fn [i [x y]]
                        ^{:key i}
                        [tool.v/square-handle {:id (keyword (str i))
                                               :x x
                                               :y y
                                               :type :handle
                                               :action :edit
                                               :element (:id el)}])]
    [:g {:key ::edit-handles}
     (map-indexed (fn [i segment]
                    (case (-> segment first str/lower-case)
                      "m"
                      (let [[x y] (mapv length/unit->px [(second segment) (last segment)])
                            [x y] (mat/add offset [x y])]
                        (square-handle i [x y]))

                      "l"
                      (let [[x y] (mapv length/unit->px [(second segment) (last segment)])
                            [x y] (mat/add offset [x y])]
                        (square-handle i [x y]))

                      nil))
                  segments)]))

(defn translate-segment
  [path i [x y]]
  (let [segment (aget (.-segments path) i)
        segment (array (aget segment 0)
                       (length/transform (aget segment 1) + x)
                       (length/transform (aget segment 2) + y))]
    (aset (.-segments path) i segment)
    path))

(defmethod hierarchy/edit :path
  [el offset handle]
  (let [index (js/parseInt (name handle))]
    (update-in el [:attrs :d] #(-> (svgpath %)
                                   (translate-segment index offset)
                                   (.toString)))))
