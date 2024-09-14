(ns renderer.tool.draw.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   ["perfect-freehand" :refer [getStroke]]
   [clojure.core.matrix :as mat]
   [clojure.core.matrix.stats :as mat.stats]
   [renderer.app.handlers :as app.h]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.attribute.range :as attr.range]
   [renderer.attribute.views :as attr.v]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :brush ::tool.hierarchy/renderable)

(defmethod tool.hierarchy/properties :brush
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


(defmethod tool.hierarchy/help [:brush :default]
  []
  "Click and drag to draw.")

(defmethod tool.hierarchy/activate :brush
  [db]
  (assoc db :cursor "none"))

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

(defmethod tool.hierarchy/pointer-move :brush
  [{:keys [adjusted-pointer-pos] :as db} {:keys [pressure]}]
  (let [[x y] adjusted-pointer-pos
        r (* (/ 16 2) (if (zero? pressure) 1 pressure))
        stroke (get-in db [:documents (:active-document db) :stroke])]
    (element.h/set-temp db {:type :element
                            :tag :circle
                            :attrs {:cx x
                                    :cy y
                                    :r r
                                    :fill stroke}})))

(defmethod tool.hierarchy/drag :brush
  [{:keys [active-document adjusted-pointer-pos] :as db} {:keys [pressure]}]
  (let [stroke (get-in db [:documents active-document :stroke])
        point (conj adjusted-pointer-pos pressure)]
    (if (get-in db [:documents active-document :temp-element :attrs :points])
      (update-in db
                 [:documents active-document :temp-element :attrs :points]
                 conj
                 point)
      (element.h/set-temp db {:type :element
                              :tag :brush
                              :attrs {:points [point]
                                      :stroke stroke
                                      :size 16
                                      :thinning 0.5
                                      :smoothing 0.5
                                      :streamline 0.5}}))))

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
               "M" (units/->fixed (first a)) "," (units/->fixed (second a))
               " Q" (units/->fixed (first b)) "," (units/->fixed (second b))
               " " (units/->fixed (mat.stats/mean [(first b) (first c)])) ","
               (units/->fixed (mat.stats/mean [(second b) (second c)])) " T")]
        (reduce-kv
         (fn [result index]
           (if (or (= len (inc index)) (< index 2))
             result
             (let [a (nth points index)
                   b (nth points (inc index))]
               (str result
                    (units/->fixed (mat.stats/mean [(first a) (first b)]))
                    ","
                    (units/->fixed (mat.stats/mean [(second a) (second b)]))
                    " ")))) d points)))))

(defn points->path
  [points options]
  (-> points
      (clj->js)
      (getStroke (clj->js options))
      (js->clj)
      (get-svg-path-from-stroke)))

(defmethod tool.hierarchy/render :brush
  [{:keys [attrs] :as element}]
  (let [pointer-handler #(pointer/event-handler % element)]
    [:path (merge {:d (points->path (:points attrs)
                                    (merge (select-keys attrs option-keys)
                                           {:simulatePressure true}))
                   :on-pointer-up pointer-handler
                   :on-pointer-down pointer-handler
                   :on-pointer-move pointer-handler
                   :on-double-click pointer-handler}
                  (-> attrs
                      (select-keys [:id :class :opacity])
                      (assoc :fill (:stroke attrs))))]))

(defmethod tool.hierarchy/bounds :brush
  [{{:keys [points]} :attrs}]
  (let [x1 (apply min (map #(units/unit->px (first %)) points))
        y1 (apply min (map #(units/unit->px (second %)) points))
        x2 (apply max (map #(units/unit->px (first %)) points))
        y2 (apply max (map #(units/unit->px (second %)) points))]
    [x1 y1 x2 y2]))

(defmethod tool.hierarchy/translate :brush
  [el [x y]]
  (update-in el
             [:attrs :points]
             #(mapv (fn [point] (mat/add point [x y 0])) %)))

(defmethod tool.hierarchy/place :brush
  [el position]
  (let [center (bounds/center (tool.hierarchy/bounds el))
        offset (mat/sub position center)]
    (tool.hierarchy/translate el offset)))

(defmethod tool.hierarchy/scale :brush
  [el ratio pivot-point]
  (let [bounds-start (take 2 (tool.hierarchy/bounds el))
        pivot-point (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (update-in el
               [:attrs :points]
               #(mapv (fn [point]
                        (let [rel-point (mat/sub (take 2 bounds-start) (take 2 point))
                              [x y] (mat/add pivot-point (mat/sub rel-point (mat/mul rel-point ratio)))]
                          (mat/add point [x y 0]))) %))))

(defmethod tool.hierarchy/drag-end :brush
  [db _e]
  (-> db
      (element.h/add)
      (app.h/set-state :default)
      (app.h/explain "Draw line")))

(defmethod tool.hierarchy/path :brush
  [{:keys [attrs]}]
  (points->path (:points attrs) (select-keys attrs option-keys)))

(defmethod tool.hierarchy/render-edit :brush
  [{:keys [attrs id] :as el} zoom]
  (let [handle-size (/ 8 zoom)
        stroke-width (/ 1 zoom)
        offset (element/offset el)]
    [:g {:key ::edit-handles}
     (map-indexed (fn [index [x y]]
                    (let [[x y] (mapv units/unit->px [x y])
                          [x y] (mat/add offset [x y])]
                      [overlay/square-handle {:id index
                                              :x x
                                              :y y
                                              :size handle-size
                                              :stroke-width stroke-width
                                              :type :handle
                                              :tag :edit
                                              :element id}]))
                  (:points attrs))]))
