(ns renderer.element.impl.custom.blob
  "Custom element for https://blobs.dev/"
  (:require
   ["blobs/v2" :as blobs]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.impl.length :as attribute.impl.length]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.event.impl.pointer :as event.impl.pointer]
   [renderer.tool.views :as tool.views]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.element :as utils.element]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.length :as utils.length]
   [renderer.utils.svg :as utils.svg]
   [renderer.views :as views]))

(derive :blob ::element.hierarchy/renderable)

(derive :size ::attribute.impl.length/length)

(defmethod attribute.hierarchy/form-element [:blob :extraPoints]
  [_ k v attrs]
  [attribute.views/range-input k v (merge attrs {:min 0
                                                 :max 50
                                                 :step 1
                                                 :placeholder 0})])

(defmethod attribute.hierarchy/form-element [:blob :randomness]
  [_ k v attrs]
  [attribute.views/range-input k v (merge attrs {:min 0
                                                 :max 50
                                                 :step 1
                                                 :placeholder 0})])

(defmethod attribute.hierarchy/form-element [:blob :seed]
  [_ k v attrs]
  (let [random-seed (rand-int 1000000)]
    [:div.flex.flex-row.gap-px.w-full
     [attribute.views/form-input k v (merge attrs {:placeholder 0})]
     [:button.form-control-button
      {:title (t [::generate-random-seed "Generate random seed"])
       :disabled (:disabled attrs)
       :on-click #(rf/dispatch [::element.events/set-attr k random-seed])}
      [views/icon "refresh"]]]))

(defmethod attribute.hierarchy/description [:blob :x]
  []
  (t [::x "Horizontal coordinate of the blob's center."]))

(defmethod attribute.hierarchy/description [:blob :y]
  []
  (t [::y "Vertical coordinate of the blob's center."]))

(defmethod attribute.hierarchy/description [:blob :seed]
  []
  (t [::seed "A given seed will always produce the same blob."]))

(defmethod attribute.hierarchy/description [:blob :extraPoints]
  []
  (t [::extra-points "The actual number of points will be `3 + extraPoints`."]))

(defmethod attribute.hierarchy/description [:blob :randomness]
  []
  (t [::randomness "Increases the amount of variation in point position."]))

(defmethod attribute.hierarchy/description [:blob :size]
  []
  (t [::size "The size of the bounding box."]))

(defmethod attribute.hierarchy/initial [:blob :extraPoints] [] "0")

(defmethod attribute.hierarchy/initial [:blob :randomness] [] "0")

(defmethod attribute.hierarchy/initial [:blob :size] [] "0")

(defmethod element.hierarchy/properties :blob
  []
  {:icon "blob"
   :description (t [::description "Vector based blob."])
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
        (attribute.hierarchy/update-attr :size * ratio)
        (element.hierarchy/translate offset))))

(defmethod element.hierarchy/render :blob
  [el]
  (let [{:keys [attrs children]} el
        child-elements @(rf/subscribe [::element.subs/filter-visible children])
        pointer-handler (partial event.impl.pointer/handler! el)]
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
  (-> el
      (attribute.hierarchy/update-attr :x + x)
      (attribute.hierarchy/update-attr :y + y)))

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
        options (->> (select-keys (:attrs el) [:seed :extraPoints :randomness :size])
                     (merge (utils.attribute/defaults-with-vals :blob))
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
    (attribute.hierarchy/update-attr el :size #(max 0 (+ % (min x y))))
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
       :action :edit
       :element-id (:id el)
       :x x2
       :y y2
       :id :size}]
     [utils.svg/times [x1 y1]]]))
