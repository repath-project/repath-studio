(ns renderer.tools.path
  "https://www.w3.org/TR/SVG/paths.html#PathElement"
  (:require
   ["paper" :refer [Path]]
   ["svg-path-bbox" :as svg-path-bbox]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as mat]
   #_[clojure.string :as str]
   [goog.object]
   [renderer.tools.base :as tools]
   [renderer.utils.bounds :as bounds]))

(derive :path ::tools/graphics)

#_(def path-commands {:m "Move To"
                      :l "Line To"
                      :v "Vertical Line"
                      :h "Horizontal Line"
                      :c "Cubic Bézier"
                      :a "Arc"
                      :z "Close Path"})

#_(defn ->command
    [char]
    (get path-commands (keyword (str/lower-case char))))

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
                                      #_(goog.object/set "fullySelected" true)
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
                               (.toString))))

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
                                 (.toString)))))

(defmethod tools/area :path
  [{{:keys [d]} :attrs}]
  d)

(defmethod tools/bounds :path
  [{{:keys [d]} :attrs}]
  (let [[left top right bottom] (js->clj (svg-path-bbox d))]
    [left top right bottom]))
