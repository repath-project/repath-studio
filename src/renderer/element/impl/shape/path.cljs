(ns renderer.element.impl.shape.path
  "https://www.w3.org/TR/SVG/paths.html#PathElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/path"
  (:require
   ["svg-path-bbox" :refer [svgPathBbox]]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.tool.views :as tool.views]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]))

(derive :path ::element.hierarchy/shape)

(defmethod element.hierarchy/properties :path
  []
  {:icon "bezier-curve"
   :description "The <path> SVG element is the generic element to define a shape.
                 All the basic shapes can be created with a path element."
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linejoin
           :stroke-linecap
           :opacity]})

(defmethod element.hierarchy/translate :path
  [el [x y]]
  (update-in el [:attrs :d] #(-> (svgpath %)
                                 (.translate x y)
                                 (.toString))))

(defmethod element.hierarchy/scale :path
  [el ratio pivot-point]
  (let [[scale-x scale-y] ratio
        offset (utils.element/scale-offset ratio pivot-point)
        [x y] (element.hierarchy/bbox el)
        [x y] (-> (matrix/add [x y] offset)
                  (matrix/sub (matrix/mul ratio [x y])))]
    (update-in el [:attrs :d] #(-> (svgpath %)
                                   (.scale scale-x scale-y)
                                   (.translate x y)
                                   (.toString)))))

(defmethod element.hierarchy/bbox :path
  [el]
  (-> el :attrs :d svgPathBbox js->clj))

(defmethod element.hierarchy/render-edit :path
  [el]
  (let [offset (utils.element/offset el)
        segments (-> el :attrs :d svgpath .-segments)
        square-handle (fn [i [x y]]
                        ^{:key i}
                        [tool.views/square-handle {:id (keyword (str i))
                                                   :x x
                                                   :y y
                                                   :type :handle
                                                   :action :edit
                                                   :element (:id el)}])]
    [:g {:key ::edit-handles}
     (map-indexed (fn [i segment]
                    (case (-> segment first string/lower-case)
                      "m"
                      (let [[x y] (mapv utils.length/unit->px [(second segment) (last segment)])
                            [x y] (matrix/add offset [x y])]
                        (square-handle i [x y]))

                      "l"
                      (let [[x y] (mapv utils.length/unit->px [(second segment) (last segment)])
                            [x y] (matrix/add offset [x y])]
                        (square-handle i [x y]))

                      nil))
                  segments)]))

(defn translate-segment
  [path i [x y]]
  (let [segment (aget (.-segments path) i)
        segment (array (aget segment 0)
                       (utils.length/transform (aget segment 1) + x)
                       (utils.length/transform (aget segment 2) + y))]
    (aset (.-segments path) i segment)
    path))

(defmethod element.hierarchy/edit :path
  [el offset handle]
  (let [index (js/parseInt (name handle))]
    (update-in el [:attrs :d] #(-> (svgpath %)
                                   (translate-segment index offset)
                                   (.toString)))))

(defmethod element.hierarchy/path :path
  [el]
  (-> el :attrs :d))
