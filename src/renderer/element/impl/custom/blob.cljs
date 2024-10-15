(ns renderer.element.impl.custom.blob
  "Custom element for https://blobs.dev/"
  (:require
   ["blobs/v2" :as blobs]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.attribute.impl.length :as length]
   [renderer.attribute.views :as attr.v]
   [renderer.element.events :as-alias element.e]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.element.subs :as-alias element.s]
   [renderer.handle.views :as handle.v]
   [renderer.ui :as ui]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.overlay :as overlay]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

  (derive :blob ::hierarchy/renderable)

  (derive :size ::length/length)

  (defmethod attr.hierarchy/form-element [:blob :extraPoints]
    [_ k v attrs]
    [attr.v/range-input k v (merge attrs {:min 0
                                          :max 50
                                          :step 1
                                          :placeholder 0})])

  (defmethod attr.hierarchy/form-element [:blob :randomness]
    [_ k v attrs]
    [attr.v/range-input k v (merge attrs {:min 0
                                          :max 50
                                          :step 1
                                          :placeholder 0})])

  (defmethod attr.hierarchy/form-element [:blob :seed]
    [_ k v {:keys [disabled] :as attrs}]
    (let [random-seed (rand-int 1000000)]
      [:div.flex.flex-row.gap-px.w-full
       [attr.v/form-input k v (merge attrs {:placeholder 0})]
       [:button.form-control-button
        {:title "Generate random seed"
         :disabled disabled
         :on-click #(rf/dispatch [::element.e/set-attr k random-seed])}
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

  (defmethod hierarchy/properties :blob
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
             :opacity]})

  (defmethod hierarchy/scale :blob
    [el ratio pivot-point]
    (let [offset (mat/sub pivot-point (mat/mul pivot-point ratio))
          ratio (apply min ratio)]
      (-> el
          (attr.hierarchy/update-attr :size * ratio)
          (hierarchy/translate offset))))

  (defmethod hierarchy/render :blob
    [el]
    (let [{:keys [attrs children]} el
          child-elements @(rf/subscribe [::element.s/filter-visible children])
          pointer-handler #(pointer/event-handler! % el)]
      [:path (merge {:d (hierarchy/path el)
                     :on-pointer-up pointer-handler
                     :on-pointer-down pointer-handler
                     :on-pointer-move pointer-handler}
                    (select-keys attrs [:stroke
                                        :fill
                                        :stroke-width
                                        :id
                                        :class
                                        :opacity])) child-elements]))

  (defmethod hierarchy/translate :blob
    [el [x y]]
    (-> el
        (attr.hierarchy/update-attr :x + x)
        (attr.hierarchy/update-attr :y + y)))

  (defmethod hierarchy/place :blob
    [el [x y]]
    (let [dimensions (bounds/->dimensions (hierarchy/bounds el))
          [cx cy] (mat/div dimensions 2)]
      (-> el
          (assoc-in [:attrs :x] (- x cx))
          (assoc-in [:attrs :y] (- y cy)))))

  (defmethod hierarchy/bounds :blob
    [el]
    (let [{{:keys [x y size]} :attrs} el
          [x y size] (mapv units/unit->px [x y size])]
      [x y (+ x size) (+ y size)]))

  (defmethod hierarchy/centroid :blob
    [el]
    (let [{{:keys [x y size]} :attrs} el
          [x y size] (mapv units/unit->px [x y size])]
      (mat/add [x y] (/ size 2))))

  (defmethod hierarchy/path :blob
    [el]
    (let [{{:keys [x y]} :attrs} el
          [x y] (mapv units/unit->px [x y])
          options (->> [:seed :extraPoints :randomness :size]
                       (select-keys (:attrs el))
                       (reduce (fn [options [k v]] (assoc options k (int v))) {})
                       (clj->js))]
      (-> blobs
          (.svgPath options)
          (svgpath)
          (.translate x y)
          (.toString))))

  (defmethod hierarchy/edit :blob
    [el [x y] handle]
    (case handle
      :size
      (attr.hierarchy/update-attr el :size #(max 0 (+ % (min x y))))
      el))

  (defmethod hierarchy/render-edit :blob
    [el]
    (let [{{:keys [x y size]} :attrs} el
          [x y size] (mapv units/unit->px [x y size])
          offset (element/offset el)
          [x1 y1] (cond->> [x y] (not (element/svg? el)) (mat/add offset))
          [x2 y2] (mat/add [x1 y1] size)]
      [:<>
       [overlay/line x1 y1 x2 y2]
       [handle.v/square
        {:type :handle
         :cursor "move"
         :action :edit
         :element (:id el)
         :x x2
         :y y2
         :id :size}]
       [overlay/times [x1 y1]]]))
