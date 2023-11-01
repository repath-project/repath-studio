(ns renderer.tools.path
  "https://www.w3.org/TR/SVG/paths.html#PathElement"
  (:require
   [renderer.tools.base :as tools]
   #_[clojure.string :as str]
   ["svgpath" :as svgpath]
   ["svg-path-bbox" :as svg-path-bbox]
   ["paper" :refer [Path]]
   [goog.object]))

(derive :path ::tools/graphics)

#_(def path-commands {"M" "Move To"
                      "L" "Line To"
                      "V" "Vertical Line"
                      "H" "Horizontal Line"
                      "C" "Cubic Bézier"
                      "A" "Arc"
                      "Z" "Close Path"})

#_(defn ->command
    [char]
    (get path-commands (str/upper-case char)))

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
  [element [x y]]
  (assoc-in element [:attrs :d] (-> (:d (:attrs element))
                                    (svgpath)
                                    (.translate x y)
                                    (.toString))))

(defmethod tools/area :path
  [{{:keys [d]} :attrs}]
  d)

(defmethod tools/bounds :path
  [{{:keys [d]} :attrs}]
  (let [[left top right bottom] (js->clj (svg-path-bbox d))]
    [left top right bottom]))