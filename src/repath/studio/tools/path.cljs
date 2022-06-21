(ns repath.studio.tools.path
  (:require [repath.studio.tools.base :as tools]
            [repath.studio.attrs.views :as attrs]
            [clojure.string :as str]
            ["svgpath" :as svgpath]
            ["svg-path-bounds" :as get-bounds]))

(derive :path ::tools/shape)

(def path-commands {"M" "Move To"
                    "L" "Line To"
                    "V" "Vertical Line"
                    "H" "Horizontal Line"
                    "C" "Cubic Bézier"
                    "A" "Arc"
                    "Z" "Close Path"})

(defn ->command
  [char]
  (get path-commands (str/upper-case char)))

(defmethod attrs/form-element :d
  [key value]
  [:div.v-box
   ^{:key value} [:input {:default-value value
                          :on-blur #(attrs/on-change-handler % key value)
                          :on-key-down #((when (= (.-keyCode %) 13) (attrs/on-change-handler % key value)))}]
   #_[:div.v-scroll {:key key
                   :style {:width "100%"
                           :max-height "300px"
                           :background-color styles/level-2}}
    [:dl (map (fn [node] [:<>
                          [:dt {:style {:font-weight "bold" :opacity ".5"}} (->command (first node))]
                          [:dd (map (fn [step]
                                      [:input {:style {:width "50%"} :value step}]) (rest node))]]) (js->clj (.-segments (svgpath value))))]]])

(defmethod tools/properties :path [] {; :icon "bezier-curve"
                                      :description "The <path> SVG element is the generic element to define a shape. All the basic shapes can be created with a path element."
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
  (let [[left top right bottom] (js->clj (get-bounds d))]
    [left top right bottom]))