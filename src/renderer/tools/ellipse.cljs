(ns renderer.tools.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [renderer.element.handlers :as elements]
   [renderer.tools.base :as tools]
   [renderer.utils.units :as units]
   [renderer.overlay :as overlay]
   [renderer.attribute.hierarchy :as hierarchy]
   [clojure.string :as str]
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]))

(derive :ellipse ::tools/shape)

(defmethod tools/properties :ellipse
  []
  {:icon "ellipse"
   :description "The <ellipse> element is an SVG basic shape, used to create 
                 ellipses based on a center coordinate, and both their x and 
                 y radius."
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray]})

(defmethod tools/drag :ellipse
  [{:keys [adjusted-mouse-offset active-document adjusted-mouse-pos] :as db}
   event]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        lock-ratio? (contains? (:modifiers event) :ctrl)
        rx (abs (- pos-x offset-x))
        ry (abs (- pos-y offset-y))
        attrs {:cx offset-x
               :cy offset-y
               :fill fill
               :stroke stroke
               :rx (if lock-ratio? (min rx ry) rx)
               :ry (if lock-ratio? (min rx ry) ry)}]
    (elements/set-temp db {:type :element
                           :tag :ellipse
                           :attrs attrs})))

(defmethod tools/translate :ellipse
  [element [x y]] (-> element
                      (hierarchy/update-attr :cx + x)
                      (hierarchy/update-attr :cy + y)))

(defmethod tools/scale :ellipse
  [element [x y] handler]
  (let [[x y] (matrix/div [x y] 2)]
    (cond-> element
      (contains? #{:bottom-right :top-right :middle-right} handler)
      (-> (hierarchy/update-attr :rx + x)
          (hierarchy/update-attr :cx + x))

      (contains? #{:bottom-left :top-left :middle-left} handler)
      (-> (hierarchy/update-attr :rx - x)
          (hierarchy/update-attr :cx + x))

      (contains? #{:bottom-middle :bottom-right :bottom-left} handler)
      (-> (hierarchy/update-attr :cy + y)
          (hierarchy/update-attr :ry + y))

      (contains? #{:top-middle :top-left :top-right} handler)
      (-> (hierarchy/update-attr :ry - y)
          (hierarchy/update-attr :cy + y)))))

(defmethod tools/bounds :ellipse
  [{{:keys [cx cy rx ry stroke-width stroke]} :attrs}]
  (let [[cx cy rx ry stroke-width-px] (map units/unit->px
                                           [cx cy rx ry stroke-width])
        stroke-width-px (if (str/blank? stroke-width) 1 stroke-width-px)
        [rx ry] (matrix/add [rx ry]
                            (/ (if (str/blank? stroke) 0 stroke-width-px) 2))]
    [(- cx rx) (- cy ry) (+ cx rx) (+ cy ry)]))

(defmethod tools/path :ellipse
  [{{:keys [cx cy rx ry]} :attrs}]
  (let [[cx cy rx ry] (mapv units/unit->px [cx cy rx ry])]
    (str/join " " ["M" (+ cx rx) cy
                   "A" rx ry 0 0 1 cx (+ cy ry)
                   "A" rx ry 0 0 1 (- cx rx) cy
                   "A" rx ry 0 0 1 (+ cx rx) cy
                   "z"])))

(defmethod tools/area :ellipse
  [{{:keys [rx ry]} :attrs}]
  (let [[rx ry] (map units/unit->px [rx ry])]
    (* Math/PI rx ry)))

(defmethod tools/edit :ellipse
  [element [x y] handler]
  (case handler
    :rx (hierarchy/update-attr element :rx #(abs (+ % x)))
    :ry (hierarchy/update-attr element :ry #(abs (- % y)))
    element))

(defmethod tools/render-edit :ellipse
  [{:keys [attrs key]}]
  (let [{:keys [cx cy rx ry]} attrs
        [cx cy rx ry] (mapv units/unit->px [cx cy rx ry])
        active-page @(rf/subscribe [:element/active-page])
        page-pos (mapv units/unit->px
                       [(-> active-page :attrs :x) (-> active-page :attrs :y)])
        [cx cy] (matrix/add page-pos [cx cy])]
    [:g :edit-handlers
     [overlay/times cx cy]
     [overlay/line cx cy (+ cx rx) cy]
     [overlay/label (str (units/->fixed rx)) [(+ cx (/ rx 2)) cy]]
     [overlay/line cx cy cx (- cy ry)]
     [overlay/label (str (units/->fixed ry)) [cx (- cy (/ ry 2))]]
     (map (fn [handler]
            [overlay/square-handler (merge handler
                                           {:type :handler
                                            :tag :edit
                                            :element key})
             ^{:key (str key "-" (:key handler))}
             [:title (:key handler)]])
          [{:x (+ cx rx) :y cy :key :rx}
           {:x cx :y (- cy ry) :key :ry}])]))
