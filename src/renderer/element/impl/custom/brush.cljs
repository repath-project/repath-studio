(ns renderer.element.impl.custom.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   ["perfect-freehand" :refer [getStroke]]
   [clojure.core.matrix :as matrix]
   [clojure.core.matrix.stats :as matrix.stats]
   [clojure.string :as string]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.impl.range :as attribute.impl.range]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.event.impl.pointer :as event.impl.pointer]
   [renderer.tool.views :as tool.views]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.element :as utils.element]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.length :as utils.length]))

(derive :brush ::element.hierarchy/renderable)

(defmethod element.hierarchy/properties :brush
  []
  {:icon "brush"
   :label (t [::name "Brush"])
   :description (t [::description "Draw pressure-sensitive freehand lines using perfect-freehand."])
   :url "https://github.com/steveruizok/perfect-freehand"
   :attrs [:points
           :stroke
           :opacity
           :size
           :thinning
           :smoothing
           :streamline
           :id
           :class]})

(defonce option-keys
  [:size :thinning :smoothing :streamline])

(derive :thinning ::attribute.impl.range/range)
(derive :smoothing :attribute.impl.range/range)
(derive :streamline ::attribute.impl.range/range)

(defmethod attribute.hierarchy/form-element [:brush :size]
  [_ k v attrs]
  [attribute.views/range-input k v (merge attrs {:min 1
                                                 :max 100
                                                 :step 1})])

(defmethod attribute.hierarchy/form-element [:brush :points]
  [_ _k v]
  [:input.form-element {:value v
                        :disabled true
                        :placeholder (when-not v "multiple")}])

(defmethod attribute.hierarchy/description [:brush ::points]
  []
  (t [::points "Input points recorded from a user's mouse movement."]))

(defmethod attribute.hierarchy/description [:brush :size]
  []
  (t [::size "The base size (diameter) of the stroke."]))

(defmethod attribute.hierarchy/description [:brush :thinning]
  []
  (t [::thinning "The effect of pressure on the stroke's size."]))

(defmethod attribute.hierarchy/description [:brush :smoothing]
  []
  (t [::smoothing "How much to soften the stroke's edges."]))

(defmethod attribute.hierarchy/description [:brush :streamline]
  []
  (t [::stream-line "How much to streamline the stroke."]))

(defn get-svg-path-from-stroke
  "Turns the points returned by getStroke into SVG path data.
   Ported from https://github.com/steveruizok/perfect-freehand#rendering"
  [points]
  (let [len (count points)]
    (if (< len 4)
      ""
      (let [a (nth points 0)
            b (nth points 1)
            c (nth points 2)
            d (str
               "M" (.toFixed (first a) 2) "," (.toFixed (second a) 2)
               " Q" (.toFixed (first b) 2) "," (.toFixed (second b) 2)
               " " (.toFixed (matrix.stats/mean [(first b) (first c)]) 2) ","
               (.toFixed (matrix.stats/mean [(second b) (second c)]) 2) " T")]
        (reduce-kv
         (fn [result index]
           (if (or (= len (inc index)) (< index 2))
             result
             (let [a (nth points index)
                   b (nth points (inc index))]
               (str result
                    (.toFixed (matrix.stats/mean [(first a) (first b)]) 2)
                    ","
                    (.toFixed (matrix.stats/mean [(second a) (second b)]) 2)
                    " ")))) d points)))))

(def partition-to-px
  (comp
   (map utils.length/unit->px)
   (partition-all 3)))

(defn points->path
  [points options]
  (-> (into [] partition-to-px (utils.attribute/str->seq points))
      (clj->js)
      (getStroke (clj->js options))
      (js->clj)
      (get-svg-path-from-stroke)))

(defmethod element.hierarchy/render :brush
  [el]
  (let [attrs (:attrs el)
        pointer-handler #(event.impl.pointer/handler! % el)
        options (-> attrs
                    (select-keys option-keys)
                    (update-vals js/parseFloat))]
    [:path (merge {:d (points->path (:points attrs) options)
                   :on-pointer-up pointer-handler
                   :on-pointer-down pointer-handler
                   :on-pointer-move pointer-handler}
                  (-> attrs
                      (select-keys [:id :class :opacity])
                      (assoc :fill (:stroke attrs))))]))

(defn points->vec
  [points]
  (utils.attribute/points->vec points 3))

(defmethod element.hierarchy/bbox :brush
  [el]
  (let [points (-> el :attrs :points points->vec)
        min-x (apply min (map #(utils.length/unit->px (first %)) points))
        min-y (apply min (map #(utils.length/unit->px (second %)) points))
        max-x (apply max (map #(utils.length/unit->px (first %)) points))
        max-y (apply max (map #(utils.length/unit->px (second %)) points))]
    [min-x min-y max-x max-y]))

(defn translate
  [[offset-x offset-y] points point]
  (let [[point-x point-y pressure] point]
    (cond-> points
      point
      (conj (utils.length/transform point-x + offset-x)
            (utils.length/transform point-y + offset-y)
            pressure))))

(defmethod element.hierarchy/translate :brush
  [el offset]
  (update-in el
             [:attrs :points]
             #(->> (utils.attribute/str->seq %)
                   (transduce (partition-all 3) (partial translate offset) [])
                   (string/join " "))))

(defmethod element.hierarchy/scale :brush
  [el ratio pivot-point]
  (let [bbox-min (take 2 (element.hierarchy/bbox el))
        offset (utils.element/scale-offset ratio pivot-point)]
    (update-in el
               [:attrs :points]
               #(->> (into [] partition-to-px (utils.attribute/str->seq %))
                     (reduce (fn [points point]
                               (let [rel-point (matrix/sub bbox-min (take 2 point))
                                     rel-offset (utils.element/scale-offset ratio
                                                                            rel-point)
                                     offset (matrix/add offset rel-offset)]
                                 (translate offset points point))) [])
                     (string/join " ")))))

(defmethod element.hierarchy/path :brush
  [el]
  (points->path (-> el :attrs :points) (select-keys (:attrs el) option-keys)))

(defmethod element.hierarchy/render-edit :brush
  [el]
  [:g (map-indexed (fn [index [x y]]
                     (let [[x y] (mapv utils.length/unit->px [x y])
                           [x y] (matrix/add (utils.element/offset el) [x y])]
                       ^{:key index}
                       [tool.views/square-handle {:id (keyword (str index))
                                                  :x x
                                                  :y y
                                                  :type :handle
                                                  :action :edit
                                                  :element (:id el)}]))
                   (-> el :attrs :points points->vec))])
