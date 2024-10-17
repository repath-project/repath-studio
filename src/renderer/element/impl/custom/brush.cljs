(ns renderer.element.impl.custom.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   ["perfect-freehand" :refer [getStroke]]
   [clojure.core.matrix :as mat]
   [clojure.core.matrix.stats :as mat.stats]
   [clojure.string :as str]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.attribute.impl.range :as attr.range]
   [renderer.attribute.views :as attr.v]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.handle.views :as handle.v]
   [renderer.utils.attribute :as attr]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :brush ::hierarchy/renderable)

(defmethod hierarchy/properties :brush
  []
  {:icon "brush"
   :description "Draw pressure-sensitive freehand lines using perfect-freehand."
   :url "https://github.com/steveruizok/perfect-freehand"
   :attrs [:points
           :stroke
           :opacity
           :size
           :thinning
           :smoothing
           :streamline]})

(defonce option-keys
  [:size :thinning :smoothing :streamline])

(derive :thinning ::attr.range/range)
(derive :smoothing ::attr.range/range)
(derive :streamline ::attr.range/range)

(defmethod attr.hierarchy/form-element [:brush :size]
  [_ k v attrs]
  [attr.v/range-input k v (merge attrs {:min 1
                                        :max 100
                                        :step 1})])

(defmethod attr.hierarchy/form-element [:brush :points]
  [_ _k v]
  [:input {:value v
           :disabled true
           :placeholder (when-not v "multiple")}])

(defmethod attr.hierarchy/description [:brush ::points]
  []
  "Input points recorded from a user's mouse movement.")

(defmethod attr.hierarchy/description [:brush :size]
  []
  "The base size (diameter) of the stroke.")

(defmethod attr.hierarchy/description [:brush :thinning]
  []
  "The effect of pressure on the stroke's size.")

(defmethod attr.hierarchy/description [:brush :smoothing]
  []
  "How much to soften the stroke's edges.")

(defmethod attr.hierarchy/description [:brush :streamline]
  []
  "How much to streamline the stroke.")

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
               " " (.toFixed (mat.stats/mean [(first b) (first c)]) 2) ","
               (.toFixed (mat.stats/mean [(second b) (second c)]) 2) " T")]
        (reduce-kv
         (fn [result index]
           (if (or (= len (inc index)) (< index 2))
             result
             (let [a (nth points index)
                   b (nth points (inc index))]
               (str result
                    (.toFixed (mat.stats/mean [(first a) (first b)]) 2)
                    ","
                    (.toFixed (mat.stats/mean [(second a) (second b)]) 2)
                    " ")))) d points)))))

(def partition-to-px
  (comp
   (map units/unit->px)
   (partition-all 3)))

(defn points->path
  [points options]
  (-> (into [] partition-to-px (attr/str->seq points))
      (clj->js)
      (getStroke (clj->js options))
      (js->clj)
      (get-svg-path-from-stroke)))

(defmethod hierarchy/render :brush
  [el]
  (let [attrs (:attrs el)
        pointer-handler #(pointer/event-handler! % el)
        options  (-> attrs (select-keys option-keys) (update-vals js/parseFloat))]
    [:path (merge {:d (points->path (:points attrs) options)
                   :on-pointer-up pointer-handler
                   :on-pointer-down pointer-handler
                   :on-pointer-move pointer-handler}
                  (-> attrs
                      (select-keys [:id :class :opacity])
                      (assoc :fill (:stroke attrs))))]))

(defn points->vec
  [points]
  (attr/points->vec points 3))

(defmethod hierarchy/bounds :brush
  [el]
  (let [points (-> el :attrs :points points->vec)
        x1 (apply min (map #(units/unit->px (first %)) points))
        y1 (apply min (map #(units/unit->px (second %)) points))
        x2 (apply max (map #(units/unit->px (first %)) points))
        y2 (apply max (map #(units/unit->px (second %)) points))]
    [x1 y1 x2 y2]))

(defn translate
  [[offset-x offset-y] points point]
  (let [[point-x point-y pressure] point]
    (cond-> points
      point
      (conj (units/transform point-x + offset-x)
            (units/transform point-y + offset-y)
            pressure))))

(defmethod hierarchy/translate :brush
  [el offset]
  (update-in el
             [:attrs :points]
             #(->> (attr/str->seq %)
                   (transduce (partition-all 3) (partial translate offset) [])
                   (str/join " "))))

(defmethod hierarchy/place :brush
  [el position]
  (let [center (bounds/center (hierarchy/bounds el))
        offset (mat/sub position center)]
    (hierarchy/translate el offset)))

(defmethod hierarchy/scale :brush
  [el ratio pivot-point]
  (let [bounds-start (take 2 (hierarchy/bounds el))
        pivot-point (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (update-in el
               [:attrs :points]
               #(->> (into [] partition-to-px (attr/str->seq %))
                     (reduce (fn [points point]
                               (let [rel-point (mat/sub bounds-start (take 2 point))
                                     offset (mat/add pivot-point (mat/sub rel-point (mat/mul rel-point ratio)))]
                                 (translate offset points point))) [])
                     (str/join " ")))))

(defmethod hierarchy/path :brush
  [el]
  (points->path (-> el :attrs :points) (select-keys (:attrs el) option-keys)))

(defmethod hierarchy/render-edit :brush
  [el]
  [:g (map-indexed (fn [index [x y]]
                     (let [[x y] (mapv units/unit->px [x y])
                           [x y] (mat/add (element/offset el) [x y])]
                       ^{:key index}
                       [handle.v/square {:id (keyword (str index))
                                         :x x
                                         :y y
                                         :type :handle
                                         :action :edit
                                         :element (:id el)}]))
                   (-> el :attrs :points points->vec))])
