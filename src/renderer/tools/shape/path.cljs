(ns renderer.tools.shape.path
  "https://www.w3.org/TR/SVG/paths.html#PathElement"
  (:require
   ["paper" :refer [Path]]
   ["svg-path-bbox" :as svg-path-bbox]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.tools.base :as tools]
   [renderer.tools.overlay :as overlay]
   [renderer.utils.element :as element]
   [renderer.utils.units :as units]))

(derive :path ::tools/shape)

#_(defn manipulate-paper-path
    [path action]
    (case action
      :simplify (.simplify path)
      :smooth (.smooth path)
      :flatten (.flatten path)
      :reverse (.reverse path)))

(defn manipulate
  [element _action]
  (update-in element [:attrs :d] #(-> (Path. %)
                                      #_(manipulate-paper-path action)
                                      (.exportSVG)
                                      (.getAttribute "d"))))

(defmethod tools/properties :path
  []
  {; :icon "bezier-curve"
   :description "The <path> SVG element is the generic element to define a shape. 
                 All the basic shapes can be created with a path element."
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linejoin
           :opacity]})

(defmethod tools/translate :path
  [el [x y]]
  (assoc-in el [:attrs :d] (-> (:attrs el)
                               :d
                               svgpath
                               (.translate x y)
                               .toString)))

(defmethod tools/scale :path
  [el ratio pivot-point]
  (let [[scale-x scale-y] ratio
        [x y] (tools/bounds el)
        [x y] (mat/sub (mat/add [x y]
                                (mat/sub pivot-point
                                         (mat/mul pivot-point ratio)))
                       (mat/mul ratio [x y]))]
    (assoc-in el [:attrs :d] (-> (:attrs el)
                                 :d
                                 svgpath
                                 (.scale scale-x scale-y)
                                 (.translate x y)
                                 .toString))))

(defmethod tools/area :path
  [{{:keys [d]} :attrs}]
  d)

(defmethod tools/bounds :path
  [{{:keys [d]} :attrs}]
  (let [[left top right bottom] (js->clj (svg-path-bbox d))]
    [left top right bottom]))

(defmethod tools/render-edit :path
  [{:keys [attrs key] :as el} zoom]
  (let [handler-size (/ 8 zoom)
        stroke-width (/ 1 zoom)
        offset (element/offset el)
        segments (-> attrs :d svgpath .-segments)
        square-handle (fn [i [x y]]
                        [overlay/square-handle {:key (str i)
                                                :x x
                                                :y y
                                                :size handler-size
                                                :stroke-width stroke-width
                                                :type :handler
                                                :tag :edit
                                                :element key}])]
    [:g {:key ::edit-handles}
     (map-indexed (fn [i segment]
                    (case (-> segment first str/lower-case)
                      "m"
                      (let [[x y] (mapv units/unit->px [(second segment) (last segment)])
                            [x y] (mat/add offset [x y])]
                        (square-handle i [x y]))

                      "l"
                      (let [[x y] (mapv units/unit->px [(second segment) (last segment)])
                            [x y] (mat/add offset [x y])]
                        (square-handle i [x y]))

                      nil))
                  segments)]))

(defn translate-segment
  [path i [x y]]
  (let [segment (aget (.-segments path) i)
        segment (array (aget segment 0)
                       (units/transform (aget segment 1) + x)
                       (units/transform (aget segment 2) + y))]
    (aset (.-segments path) i segment)
    path))

(defmethod tools/edit :path
  [el offset handler]
  (cond-> el
    (not (keyword? handler))
    (update-in
     [:attrs :d]
     #(-> (svgpath %)
          (translate-segment (int handler) offset)
          .toString))))
