(ns renderer.tools.draw.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   ["perfect-freehand" :refer [getStroke]]
   ["svg-path-bbox" :as svg-path-bbox]
   [clojure.core.matrix :as mat]
   [clojure.core.matrix.stats :as mat.stats]
   [re-frame.core :as rf]
   [renderer.attribute.color :as attr.color]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.attribute.range :as attr.range]
   [renderer.attribute.views :as attr.v]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.tools.base :as tools]
   [renderer.tools.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :brush ::tools/draw)

(derive ::stroke ::attr.color/color)

(defmethod tools/properties :brush
  []
  {:icon "brush"
   :description "Draw pressure-sensitive freehand lines using perfect-freehand."
   :url "https://github.com/steveruizok/perfect-freehand"
   :attrs [::points
           ::stroke
           :opacity
           ::size
           ::thinning
           ::smoothing
           ::streamline]})

(defmethod tools/activate :brush
  [db]
  (-> db
      (assoc :cursor "crosshair")
      (h/set-message [:div "Click and drag to draw."])))

(defonce options
  [::size ::thinning ::smoothing ::streamline])

(derive ::thinning ::attr.range/range)
(derive ::smoothing ::attr.range/range)
(derive ::streamline ::attr.range/range)

(defmethod attr.hierarchy/form-element ::size
  [k v disabled?]
  [attr.v/range-input k v {:disabled disabled?
                           :min 1
                           :max 100
                           :step 1}])

(defmethod attr.hierarchy/form-element ::points
  [value]
  [:input {:value value
           :disabled true
           :placeholder (when-not value "multiple")}])

(defmethod attr.hierarchy/description ::points
  []
  "Input points recorded from a user's mouse movement.")

(defmethod attr.hierarchy/description ::size
  []
  "The base size (diameter) of the stroke.")

(defmethod attr.hierarchy/description ::thinning
  []
  "The effect of pressure on the stroke's size.")

(defmethod attr.hierarchy/description ::smoothing
  []
  "How much to soften the stroke's edges.")

(defmethod attr.hierarchy/description ::streamline
  []
  "How much to streamline the stroke.")

(defmethod tools/drag-start :brush
  [db]
  (h/set-state db :create))

(defmethod tools/drag :brush
  [{:keys [active-document
           adjusted-pointer-pos] :as db} {:keys [pressure]}]
  (let [stroke (get-in db [:documents active-document :stroke])
        point (conj adjusted-pointer-pos pressure)]
    (if (get-in db [:documents active-document :temp-element :attrs ::points])
      (update-in db
                 [:documents active-document :temp-element :attrs ::points]
                 conj
                 point)
      (element.h/set-temp db {:type :element
                              :tag :brush
                              :attrs {::points [point]
                                      ::stroke stroke
                                      ::size 16
                                      ::thinning 0.5
                                      ::smoothing 0.5
                                      ::streamline 0.5}}))))

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
      clj->js
      (getStroke (clj->js options))
      js->clj
      get-svg-path-from-stroke))

(defmethod tools/render :brush
  [{:keys [attrs] :as element}]
  (let [pointer-handler #(pointer/event-handler % element)]
    [:path (merge {:d (points->path (::points attrs)
                                    (merge (select-keys attrs options)
                                           {:simulatePressure false}))
                   :on-pointer-up pointer-handler
                   :on-pointer-down pointer-handler
                   :on-pointer-move pointer-handler
                   :on-double-click pointer-handler}
                  (-> attrs
                      (select-keys [:id :class :opacity])
                      (assoc :fill (::stroke attrs))))]))

(defmethod tools/bounds :brush
  [{:keys [attrs]}]
  (-> (::points attrs)
      (points->path (select-keys attrs options))
      svg-path-bbox
      js->clj))

(defmethod tools/translate :brush
  [el [x y]]
  (update-in el
             [:attrs ::points]
             #(mapv (fn [point] (mat/add point [x y 0])) %)))

(defmethod tools/position :brush
  [el position]
  (let [center (bounds/center (tools/bounds el))
        [x y] (mat/sub position center)]
    (update-in el
               [:attrs ::points]
               #(mapv (fn [point] (mat/add point [x y 0])) %))))

(defmethod tools/scale :brush
  [el ratio pivot-point]
  (let [bounds-start (take 2 (tools/bounds el))
        pivot-point (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (update-in el
               [:attrs ::points]
               #(mapv (fn [point]
                        (let [rel-point (mat/sub (take 2 bounds-start) (take 2 point))
                              [x y] (mat/add pivot-point (mat/sub rel-point (mat/mul rel-point ratio)))]
                          (mat/add point [x y 0]))) %))))

(defmethod tools/drag-end :brush
  [db]
  (-> db
      element.h/add
      (h/set-state :default)
      (history.h/finalize "Draw line")))

(defmethod tools/path :brush
  [{:keys [attrs]}]
  (points->path (::points attrs) (select-keys attrs options)))

(defmethod tools/render-edit :brush
  [{:keys [attrs key] :as el} zoom]
  (let [handler-size (/ 8 zoom)
        stroke-width (/ 1 zoom)
        offset @(rf/subscribe [:element/el-offset el])]
    [:g {:key :edit-handlers}
     (map-indexed (fn [index [x y]]
                    (let [[x y] (mapv units/unit->px [x y])
                          [x y] (mat/add offset [x y])]
                      [overlay/square-handler {:key (str index)
                                               :x x
                                               :y y
                                               :size handler-size
                                               :stroke-width stroke-width
                                               :type :handler
                                               :tag :edit
                                               :element key}]))
                  (::points attrs))]))
