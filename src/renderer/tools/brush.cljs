(ns renderer.tools.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   ["perfect-freehand" :refer [getStroke]]
   ["svg-path-bbox" :as svg-path-bbox]
   [clojure.core.matrix :as mat]
   [goog.math]
   [renderer.attribute.color :as color]
   [renderer.attribute.hierarchy :as attr-hierarchy]
   [renderer.attribute.range :as range]
   [renderer.attribute.views :as attr-views]
   [renderer.element.handlers :as elements]
   [renderer.handlers :as handlers]
   [renderer.history.handlers :as history]
   [renderer.overlay :as overlay]
   [renderer.tools.base :as tools]
   [renderer.utils.mouse :as mouse]
   [renderer.utils.units :as units]))

(derive :brush ::tools/draw)

(derive ::stroke ::color/color)

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

(defonce options
  [::size ::thinning ::smoothing ::streamline])

(derive ::thinning ::range/range)
(derive ::smoothing ::range/range)
(derive ::streamline ::range/range)

(defmethod attr-hierarchy/form-element ::size
  [key value disabled?]
  [attr-views/range-input key value {:disabled disabled?
                                     :min 1
                                     :max 100
                                     :step 1}])

(defmethod attr-hierarchy/form-element ::points
  [value]
  [:input {:value value
           :disabled true
           :placeholder (when-not value "multiple")}])

(defmethod attr-hierarchy/description ::points
  []
  "Input points recorded from a user's mouse movement.")

(defmethod attr-hierarchy/description ::size
  []
  "The base size (diameter) of the stroke.")

(defmethod attr-hierarchy/description ::thinning
  []
  "The effect of pressure on the stroke's size.")

(defmethod attr-hierarchy/description ::smoothing
  []
  "How much to soften the stroke's edges.")

(defmethod attr-hierarchy/description ::streamline
  []
  "How much to streamline the stroke.")

(defmethod tools/drag-start :brush
  [db]
  (handlers/set-state db :create))

(defmethod tools/drag :brush
  [{:keys [active-document
           adjusted-mouse-pos] :as db} {:keys [pressure]}]
  (let [stroke (get-in db [:documents active-document :stroke])]
    (if (get-in db [:documents active-document :temp-element :attrs ::points])
      (update-in db
                 [:documents active-document :temp-element :attrs ::points]
                 conj
                 (conj adjusted-mouse-pos pressure))
      (elements/set-temp db {:type :element
                             :tag :brush
                             :attrs {::points [(conj adjusted-mouse-pos pressure)]
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
               " " (units/->fixed (goog.math/average (first b) (first c))) ","
               (units/->fixed (goog.math/average (second b) (second c))) " T")]
        (reduce-kv
         (fn [result index]
           (if (or (= len (inc index)) (< index 2))
             result
             (let [a (nth points index)
                   b (nth points (inc index))]
               (str result
                    (units/->fixed (goog.math/average (first a)
                                                      (first b)))
                    ","
                    (units/->fixed (goog.math/average (second a)
                                                      (second b)))
                    " ")))) d points)))))

(defn points->path
  [points options]
  (-> (clj->js points)
      (getStroke (clj->js options))
      (js->clj)
      (get-svg-path-from-stroke)))

(defmethod tools/render :brush
  [{:keys [attrs] :as element}]
  (let [mouse-handler #(mouse/event-handler % element)]
    [:path (merge {:d (points->path (::points attrs)
                                    (merge (select-keys attrs options)
                                           {:simulatePressure false}))
                   :on-pointer-up mouse-handler
                   :on-pointer-down mouse-handler
                   :on-pointer-move mouse-handler
                   :on-double-click mouse-handler}
                  (-> attrs
                      (select-keys [:id :class :opacity])
                      (assoc :fill (::stroke attrs))))]))

(defmethod tools/bounds :brush
  [{:keys [attrs]}]
  (-> (::points attrs)
      (points->path (select-keys attrs options))
      (svg-path-bbox)
      (js->clj)))

(defmethod tools/translate :brush
  [element [x y]]
  (update-in element
             [:attrs ::points]
             #(mapv (fn [point] (mat/add point [x y 0])) %)))

(defmethod tools/drag-end :brush
  [db]
  (-> db
      (elements/create)
      (history/finalize (str "Draw line"))))

(defmethod tools/path :brush
  [{:keys [attrs]}]
  (points->path (::points attrs) (select-keys attrs options)))

(defmethod tools/render-edit :brush
  [{:keys [attrs key]}]
  [:g {:key :edit-handlers}
   (map (fn [point]
          [overlay/square-handler {:x (first point)
                                   :y (second point)
                                   :element key}]) (::points attrs))])
