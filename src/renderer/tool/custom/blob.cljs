(ns renderer.tool.custom.blob
  "Custom element for https://blobs.dev/"
  (:require
   ["blobs/v2" :as blobs]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.attribute.length :as length]
   [renderer.attribute.views :as attr.v]
   [renderer.element.events :as-alias element.e]
   [renderer.element.handlers :as element.h]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.base :as tool]
   [renderer.tool.overlay :as overlay]
   [renderer.ui :as ui]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :blob ::tool/renderable)

(derive :size ::length/length)

(defmethod attr.hierarchy/form-element [:blob :extraPoints]
  [_ k v disabled?]
  [attr.v/range-input k v {:min 0
                           :max 50
                           :step 1
                           :disabled disabled?} 0])

(defmethod attr.hierarchy/form-element [:blob :randomness]
  [_ k v disabled?]
  [attr.v/range-input k v {:min 0
                           :max 50
                           :step 1
                           :disabled disabled?} 0])

(defmethod attr.hierarchy/form-element [:blob :seed]
  [_ k v disabled?]
  (let [random-seed (rand-int 1000000)]
    [:<>
     [attr.v/form-input {:key k
                         :value v
                         :disabled? disabled?
                         :placeholder 0}]
     [:button.button.ml-px.inline-block.bg-primary.text-muted
      {:title "Generate random seed"
       :disabled disabled?
       :style {:flex "0 0 26px"
               :height "100%"}
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

(defmethod tool/properties :blob
  []
  {:icon "blob"
   :description "Vector based blob."
   :url "https://blobs.dev/"
   :locked-ratio? true
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

(defmethod tool/drag-start :blob
  [{:keys [adjusted-pointer-offset
           active-document
           adjusted-pointer-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-pointer-offset
        radius (mat/distance adjusted-pointer-pos adjusted-pointer-offset)]
    (element.h/set-temp db {:type :element
                            :tag :blob
                            :attrs {:x (- offset-x radius)
                                    :y (- offset-y radius)
                                    :seed (rand-int 1000000)
                                    :extraPoints 8
                                    :randomness 4
                                    :size (* radius 2)
                                    :fill fill
                                    :stroke stroke}})))

(defmethod tool/drag :blob
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos] :as db}]
  (let [[offset-x offset-y] adjusted-pointer-offset
        radius (mat/distance adjusted-pointer-pos adjusted-pointer-offset)
        temp (-> (element.h/get-temp db)
                 (assoc-in [:attrs :x] (- offset-x radius))
                 (assoc-in [:attrs :y] (- offset-y radius))
                 (assoc-in [:attrs :size] (* radius 2)))]
    (element.h/set-temp db temp)))

(defmethod tool/scale :blob
  [el ratio pivot-point]
  (let [offset (mat/sub pivot-point (mat/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :size * ratio)
        (tool/translate offset))))

(defmethod tool/render :blob
  [{:keys [attrs children] :as element}]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible children])
        pointer-handler #(pointer/event-handler % element)]
    [:path (merge {:d (tool/path element)
                   :on-pointer-up pointer-handler
                   :on-pointer-down pointer-handler
                   :on-pointer-move pointer-handler}
                  (select-keys attrs [:stroke
                                      :fill
                                      :stroke-width
                                      :id
                                      :class
                                      :opacity])) child-elements]))

(defmethod tool/translate :blob
  [element [x y]]
  (-> element
      (attr.hierarchy/update-attr :x + x)
      (attr.hierarchy/update-attr :y + y)))

(defmethod tool/position :blob
  [el [x y]]
  (let [dimensions (bounds/->dimensions (tool/bounds el))
        [cx cy] (mat/div dimensions 2)]
    (-> el
        (assoc-in [:attrs :x] (- x cx))
        (assoc-in [:attrs :y] (- y cy)))))

(defmethod tool/bounds :blob
  [{:keys [attrs]}]
  (let [{:keys [x y size]} attrs
        [x y size] (mapv units/unit->px [x y size])]
    [x y (+ x size) (+ y size)]))

(defmethod tool/centroid :blob
  [{{:keys [x y size]} :attrs}]
  (let [[x y size] (mapv units/unit->px [x y size])]
    (mat/add [x y] (/ size 2))))

(defmethod tool/path :blob
  [{:keys [attrs]}]
  (let [[x y] (mapv units/unit->px [(:x attrs) (:y attrs)])
        options (->> [:seed :extraPoints :randomness :size]
                     (select-keys attrs)
                     (reduce (fn [options [k v]] (assoc options k (int v))) {})
                     clj->js)]
    (-> blobs
        (.svgPath options)
        svgpath
        (.translate x y)
        .toString)))

(defmethod tool/edit :blob
  [element [x y] handle]
  (case handle
    :size
    (attr.hierarchy/update-attr element :size #(max 0 (+ % (min x y))))
    element))

(defmethod tool/render-edit :blob
  [{:keys [attrs id] :as el}]
  (let [{:keys [x y size]} attrs
        [x y size] (mapv units/unit->px [x y size])
        offset (element/offset el)
        [x1 y1] (cond->> [x y] (not (element/svg? el)) (mat/add offset))
        [x2 y2] (mat/add [x1 y1] size)]
    [:<>
     [overlay/line x1 y1 x2 y2]
     [overlay/square-handle
      {:type :handle
       :cursor "move"
       :tag :edit
       :element id
       :x x2
       :y y2
       :id :size}]
     [overlay/times
      [x1 y1]]]))
