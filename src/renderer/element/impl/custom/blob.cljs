(ns renderer.element.impl.custom.blob
  "Custom element for https://blobs.dev/"
  (:require
   ["blobs/v2" :as blobs]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.attribute.impl.length :as attr.length]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.tool.views :as tool.views]
   [renderer.ui :as ui]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]
   [renderer.utils.pointer :as utils.pointer]
   [renderer.utils.svg :as utils.svg]))

(derive :blob ::element.hierarchy/renderable)

(derive :size ::attr.length/length)

(defmethod attr.hierarchy/form-element [:blob :extraPoints]
  [_ k v attrs]
  [attribute.views/range-input k v (merge attrs {:min 0
                                                 :max 50
                                                 :step 1
                                                 :placeholder 0})])

(defmethod attr.hierarchy/form-element [:blob :randomness]
  [_ k v attrs]
  [attribute.views/range-input k v (merge attrs {:min 0
                                                 :max 50
                                                 :step 1
                                                 :placeholder 0})])

(defmethod attr.hierarchy/form-element [:blob :seed]
  [_ k v {:keys [disabled] :as attrs}]
  (let [random-seed (rand-int 1000000)]
    [:div.flex.flex-row.gap-px.w-full
     [attribute.views/form-input k v (merge attrs {:placeholder 0})]
     [:button.form-control-button
      {:title "Generate random seed"
       :disabled disabled
       :on-click #(rf/dispatch [::element.events/set-attr k random-seed])}
      [ui/icon "refresh"]]]))

(defmethod attr.hierarchy/description [:blob :x]
  []
  "Horizontal coordinate of the blob's center.")

(defmethod attr.hierarchy/description [:blob :y]
  []
  "Vertical coordinate of the blob's center.")

(defmethod attr.hierarchy/description [:blob :seed]
  []
  "A given seed will always produce the same blob.")

(defmethod attr.hierarchy/description [:blob :extraPoints]
  []
  "The actual number of points will be `3 + extraPoints`.")

(defmethod attr.hierarchy/description [:blob :randomness]
  []
  "Increases the amount of variation in point position.")

(defmethod attr.hierarchy/description [:blob :size]
  []
  "The size of the bounding box.")

(defmethod element.hierarchy/properties :blob
  []
  {:icon "blob"
   :description "Vector based blob."
   :url "https://blobs.dev/"
   :ratio-locked true
   :attrs [:x
           :y
           :seed
           :extraPoints
           :randomness
           :size
           :fill
           :stroke
           :stroke-width
           :opacity
           :id
           :class]})

(defmethod element.hierarchy/scale :blob
  [el ratio pivot-point]
  (let [offset (utils.element/scale-offset ratio pivot-point)
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :size * ratio)
        (element.hierarchy/translate offset))))

(defmethod element.hierarchy/render :blob
  [el]
  (let [{:keys [attrs children]} el
        child-elements @(rf/subscribe [::element.subs/filter-visible children])
        pointer-handler #(utils.pointer/event-handler! % el)]
    [:path (merge {:d (element.hierarchy/path el)
                   :on-pointer-up pointer-handler
                   :on-pointer-down pointer-handler
                   :on-pointer-move pointer-handler}
                  (select-keys attrs [:stroke
                                      :fill
                                      :stroke-width
                                      :id
                                      :class
                                      :opacity])) child-elements]))

(defmethod element.hierarchy/translate :blob
  [el [x y]]
  (utils.element/update-attrs-with el + [[:x x]
                                         [:y y]]))

(defmethod element.hierarchy/bbox :blob
  [el]
  (let [{{:keys [x y size]} :attrs} el
        [x y size] (mapv utils.length/unit->px [x y size])]
    [x y (+ x size) (+ y size)]))

(defmethod element.hierarchy/centroid :blob
  [el]
  (let [{{:keys [x y size]} :attrs} el
        [x y size] (mapv utils.length/unit->px [x y size])]
    (matrix/add [x y] (/ size 2))))

(defmethod element.hierarchy/path :blob
  [el]
  (let [{{:keys [x y]} :attrs} el
        [x y] (mapv utils.length/unit->px [x y])
        options (->> [:seed :extraPoints :randomness :size]
                     (select-keys (:attrs el))
                     (reduce (fn [options [k v]] (assoc options k (int v))) {})
                     (clj->js))]
    (-> blobs
        (.svgPath options)
        (svgpath)
        (.translate x y)
        (.toString))))

(defmethod element.hierarchy/edit :blob
  [el [x y] handle]
  (case handle
    :size
    (attr.hierarchy/update-attr el :size #(max 0 (+ % (min x y))))
    el))

(defmethod element.hierarchy/render-edit :blob
  [el]
  (let [{{:keys [x y size]} :attrs} el
        [x y size] (mapv utils.length/unit->px [x y size])
        offset (utils.element/offset el)
        [x1 y1] (cond->> [x y] (not (utils.element/svg? el)) (matrix/add offset))
        [x2 y2] (matrix/add [x1 y1] size)]
    [:<>
     [utils.svg/line [x1 y1] [x2 y2]]
     [tool.views/square-handle
      {:type :handle
       :cursor "move"
       :action :edit
       :element (:id el)
       :x x2
       :y y2
       :id :size}]
     [utils.svg/times [x1 y1]]]))
