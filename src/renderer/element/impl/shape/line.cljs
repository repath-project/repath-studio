(ns renderer.element.impl.shape.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/line"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.tool.views :as tool.views]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.length :as utils.length]
   [renderer.utils.svg :as utils.svg]))

(derive :line ::element.hierarchy/shape)

(defmethod element.hierarchy/properties :line
  []
  {:icon "line"
   :label (t [::name "Line"])
   :description (t [::description
                    "The <line> element is an SVG basic shape
                     used to create a line connecting two points."])
   :attrs [:stroke
           :stroke-width
           :stroke-linecap
           :stroke-dasharray
           :opacity]})

(defmethod element.hierarchy/translate :line
  [el [x y]]
  (utils.element/update-attrs-with el + [[:x1 x] [:y1 y] [:x2 x] [:y2 y]]))

(defmethod element.hierarchy/scale :line
  [el ratio pivot-point]
  (let [{:keys [x1 y1 x2 y2]} (:attrs el)
        [x1 y1 x2 y2] (mapv utils.length/unit->px [x1 y1 x2 y2])
        dimensions (utils.bounds/->dimensions (element.hierarchy/bbox el))
        [x y] (matrix/sub dimensions (matrix/mul dimensions ratio))
        pivot-diff (matrix/sub pivot-point dimensions)
        offset (utils.element/scale-offset ratio pivot-diff)]
    (-> (utils.element/update-attrs-with el + [[(if (< x1 x2) :x1 :x2) x]
                                               [(if (< y1 y2) :y1 :y2) y]])
        (element.hierarchy/translate offset))))

(defmethod element.hierarchy/path :line
  [el]
  (let [{{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv utils.length/unit->px [x1 y1 x2 y2])]
    (string/join " " ["M" x1 y1
                      "L" x2 y2])))

(defmethod element.hierarchy/render-edit :line
  [el]
  (let [clicked-element @(rf/subscribe [::app.subs/clicked-element])
        zoom @(rf/subscribe [::document.subs/zoom])
        margin (/ 15 zoom)
        offset (utils.element/offset el)
        {{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv utils.length/unit->px [x1 y1 x2 y2])
        [x1 y1] (matrix/add offset [x1 y1])
        [x2 y2] (matrix/add offset [x2 y2])]
    [:g
     {:key ::edit-handles}
     (map (fn [handle]
            (let [{:keys [x y id]} handle
                  is-active (and (= (:id clicked-element) id)
                                 (= (:element clicked-element) (:id el)))]
              ^{:key id}
              [:g
               [tool.views/square-handle handle
                [:title {:key (str id "-title")} (name id)]]
               (when is-active
                 [utils.svg/label
                  (string/join " " [(utils.length/->fixed x 2 false)
                                    (utils.length/->fixed y 2 false)])
                  [(- x margin) (+ y margin)]
                  "end"])]))
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

(defmethod element.hierarchy/edit :line
  [el [x y] handle]
  (case handle
    :starting-point
    (utils.element/update-attrs-with el + [[:x1 x] [:y1 y]])

    :ending-point
    (utils.element/update-attrs-with el + [[:x2 x] [:y2 y]])

    el))

(defmethod element.hierarchy/bbox :line
  [el]
  (let [{{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv utils.length/unit->px [x1 y1 x2 y2])]
    [(min x1 x2) (min y1 y2) (max x1 x2) (max y1 y2)]))

#_(defmethod hierarchy/snapping-points :line
    [el]
    (let [{{:keys [x1 y1 x2 y2]} :attrs} el
          [x1 y1 x2 y2] (mapv utils.length/unit->px [x1 y1 x2 y2])]
      [(with-meta [x1 y1] {:label "line start"})
       (with-meta [x2 y2] {:label "line end"})]))
