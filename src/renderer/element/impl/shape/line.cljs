(ns renderer.element.impl.shape.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/line"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.tool.views :as tool.v]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.length :as length]))

(derive :line ::hierarchy/shape)

(defmethod hierarchy/properties :line
  []
  {:icon "line"
   :description "The <line> element is an SVG basic shape used to create a line
                 connecting two points."
   :attrs [:stroke
           :stroke-width
           :stroke-linecap
           :stroke-dasharray
           :opacity]})

(defmethod hierarchy/translate :line
  [el [x y]]
  (element/update-attrs-with el + [[:x1 x] [:y1 y] [:x2 x] [:y2 y]]))

(defmethod hierarchy/scale :line
  [el ratio pivot-point]
  (let [{:keys [x1 y1 x2 y2]} (:attrs el)
        [x1 y1 x2 y2] (mapv length/unit->px [x1 y1 x2 y2])
        dimensions (bounds/->dimensions (hierarchy/bbox el))
        [x y] (mat/sub dimensions (mat/mul dimensions ratio))
        pivot-diff (mat/sub pivot-point dimensions)
        offset (mat/sub pivot-diff (mat/mul pivot-diff ratio))]
    (-> (element/update-attrs-with el + [[(if (< x1 x2) :x1 :x2) x]
                                         [(if (< y1 y2) :y1 :y2) y]])
        (hierarchy/translate offset))))

(defmethod hierarchy/path :line
  [el]
  (let [{{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv length/unit->px [x1 y1 x2 y2])]
    (str/join " " ["M" x1 y1
                   "L" x2 y2])))

(defmethod hierarchy/render-edit :line
  [el]
  (let [offset (element/offset el)
        {{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv length/unit->px [x1 y1 x2 y2])
        [x1 y1] (mat/add offset [x1 y1])
        [x2 y2] (mat/add offset [x2 y2])]
    [:g
     {:key ::edit-handles}
     (map (fn [handle]
            ^{:key (:id handle)}
            [tool.v/square-handle handle
             [:title {:key (str (:id handle) "-title")} (name (:id handle))]])
          [{:x x1
            :y y1
            :id :starting-point
            :type :handle
            :action :edit
            :element (:id el)}
           {:x x2
            :y y2
            :id :ending-point
            :type :handle
            :action :edit
            :element (:id el)}])]))

(defmethod hierarchy/edit :line
  [el [x y] handle]
  (case handle
    :starting-point
    (element/update-attrs-with el + [[:x1 x] [:y1 y]])

    :ending-point
    (element/update-attrs-with el + [[:x2 x] [:y2 y]])

    el))

(defmethod hierarchy/bbox :line
  [el]
  (let [{{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv length/unit->px [x1 y1 x2 y2])]
    [(min x1 x2) (min y1 y2) (max x1 x2) (max y1 y2)]))

#_(defmethod hierarchy/snapping-points :line
    [el]
    (let [{{:keys [x1 y1 x2 y2]} :attrs} el
          [x1 y1 x2 y2] (mapv length/unit->px [x1 y1 x2 y2])]
      [(with-meta [x1 y1] {:label "line start"})
       (with-meta [x2 y2] {:label "line end"})]))
