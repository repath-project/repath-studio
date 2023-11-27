(ns renderer.tools.box
  "This serves as an abstraction for box elements that share the
   :x :y :width :height attributes."
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.overlay :as overlay]
   [renderer.tools.base :as tools]
   [renderer.utils.units :as units]))

(derive ::tools/box ::tools/element)

(defmethod tools/translate ::tools/box
  [el [x y]] (-> el
                 (hierarchy/update-attr :x + x)
                 (hierarchy/update-attr :y + y)))

(defmethod tools/scale ::tools/box
  [el ratio pivot-point]
  (let [[x y] ratio
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (-> el
        (hierarchy/update-attr :width * x)
        (hierarchy/update-attr :height * y)
        (tools/translate offset))))

(defmethod tools/edit ::tools/element
  [el [x y] handler]
  (case handler
    :position (-> el
                  (hierarchy/update-attr :width #(max 0 (- % x)))
                  (hierarchy/update-attr :height #(max 0 (- % y)))
                  (tools/translate [x y]))
    :size (-> el
              (hierarchy/update-attr :width #(max 0 (+ % x)))
              (hierarchy/update-attr :height #(max 0 (+ % y))))))

(defmethod tools/render-edit ::tools/box
  [{:keys [attrs key] :as el}]
  (let [{:keys [x y width height]} attrs
        [x y width height] (mapv units/unit->px [x y width height])
        active-page @(rf/subscribe [:element/active-page])
        page-pos (mapv units/unit->px [(-> active-page :attrs :x)
                                       (-> active-page :attrs :y)])
        [x y] (cond-> [x y] (not= (:tag el) :page) (mat/add page-pos))]
    [:g {:key :edit-handlers}
     (map (fn [handler]
            (let [handler (merge handler {:type :handler
                                          :tag :edit
                                          :element key})]
              [overlay/square-handler handler]))
          [{:x x :y y :key :position}
           {:x (+ x width) :y (+ y height) :key :size}])]))

(defmethod tools/bounds ::tools/box
  [{:keys [attrs]}]
  (let [{:keys [x y width height stroke-width stroke]} attrs
        [x y width height stroke-width-px] (mapv units/unit->px [x y width height stroke-width])
        stroke-width-px (if (str/blank? stroke-width) 1 stroke-width-px)
        [x y] (mat/sub [x y] (/ (if (str/blank? stroke) 0 stroke-width-px) 2))
        [width height] (cond-> [width height]
                         (str/blank? stroke) (mat/add stroke-width-px))]
    [x y (+ x width) (+ y height)]))

(defmethod tools/area ::tools/box
  [{{:keys [width height]} :attrs}]
  (apply * (map units/unit->px [width height])))
