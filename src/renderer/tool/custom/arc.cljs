(ns renderer.tool.custom.arc
  (:require
   ["svg-path-bbox" :refer [svgPathBbox]]
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.attribute.angle :as angle]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as element.h]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.math :as math]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :arc ::tool.hierarchy/custom)
(derive :start-deg ::angle/angle)
(derive :end-deg ::angle/angle)

(defmethod tool.hierarchy/properties :arc
  []
  {:icon "arc"
   :description "Draw arcs"
   :attrs [:cx
           :cy
           :rx
           :ry
           :start-deg
           :end-deg
           :stroke-width
           :opacity
           :fill
           :stroke
           :stroke-linecap
           :stroke-dasharray]})

(defmethod tool.hierarchy/drag :arc
  [{:keys [adjusted-pointer-offset active-document adjusted-pointer-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        attrs {:cx offset-x
               :cy offset-y
               :start-deg 90
               :end-deg 180
               :fill fill
               :stroke stroke
               :rx (abs (- pos-x offset-x))
               :ry (abs (- pos-y offset-y))}]
    (element.h/assoc-temp db {:type :element :tag :arc :attrs attrs})))

(defmethod tool.hierarchy/translate :arc
  [el [x y]] (-> el
                 (attr.hierarchy/update-attr :cx + x)
                 (attr.hierarchy/update-attr :cy + y)))

(defmethod tool.hierarchy/bounds :arc
  [el]
  (let [[left top right bottom] (js->clj (svgPathBbox (tool.hierarchy/path el)))]
    [left top right bottom]))

(defmethod tool.hierarchy/area :arc
  [el]
  (-> (get-in el [:attrs :r])
      (units/unit->px)
      (Math/pow 2)
      (* Math/PI)))

(defmethod tool.hierarchy/path :arc
  [{{:keys [cx cy rx ry start-deg end-deg]} :attrs}]
  (let [[cx cy rx ry] (map units/unit->px [cx cy rx ry])
        x1 (+ cx (math/angle-dx start-deg rx))
        y1 (+ cy (math/angle-dy start-deg ry))
        x2 (+ cx (math/angle-dx end-deg rx))
        y2 (+ cy (math/angle-dy end-deg ry))
        diff-deg (- end-deg start-deg)]
    (str "M" x1 "," y1 " "
         "A" rx "," ry " 0 " (if (>= diff-deg 180) 1 0) ",1 " x2 "," y2)))

(defmethod tool.hierarchy/edit :arc
  [el [x y] handle]
  (case handle
    :rx (attr.hierarchy/update-attr el :rx #(abs (+ % x)))
    :ry (attr.hierarchy/update-attr el :ry #(abs (- % y)))
    el))

(defmethod tool.hierarchy/render :arc
  [el]
  (let [children @(rf/subscribe [::element.s/filter-visible (:children el)])
        pointer-handler #(pointer/event-handler! % el)]
    [:path (merge {:d (tool.hierarchy/path el)
                   :on-pointer-up pointer-handler
                   :on-pointer-down pointer-handler
                   :on-pointer-move pointer-handler}
                  (select-keys (:attrs el) [:stroke-width
                                            :opacity
                                            :fill
                                            :stroke
                                            :stroke-linecap
                                            :stroke-dasharray])) children]))

(defmethod tool.hierarchy/render-edit :arc
  [el]
  (let [{:keys [attrs id]} el
        {:keys [cx cy rx ry start-deg end-deg]} attrs
        [cx cy rx ry] (mapv units/unit->px [cx cy rx ry])
        active-page @(rf/subscribe [::element.s/active-page])
        x1 (+ cx (math/angle-dx start-deg rx))
        y1 (+ cy (math/angle-dy start-deg ry))
        x2 (+ cx (math/angle-dx end-deg rx))
        y2 (+ cy (math/angle-dy end-deg ry))
        page-pos (mapv
                  units/unit->px
                  [(-> active-page :attrs :x) (-> active-page :attrs :y)])
        [[cx cy] [x1 y1] [x2 y2]] (mat/add page-pos [[cx cy] [x1 y1] [x2 y2]])
        zoom @(rf/subscribe [::document.s/zoom])]
    [:g
     [overlay/times cx cy]
     [overlay/line cx cy (+ cx rx) cy]
     [overlay/label (str (.toFixed rx 2)) [(+ cx (/ rx 2)) cy]]
     [overlay/line cx cy cx (- cy ry)]
     [overlay/label (str (.toFixed ry 2)) [cx (- cy (/ ry 2))]]
     [:ellipse {:cx cx
                :cy cy
                :rx rx
                :ry ry
                :fill "transparent"
                :stroke-dasharray 2
                :stroke "#555"
                :stroke-width (/ 1 zoom)}]
     (map (fn [handle] [overlay/circle-handle handle])
          [{:x x1 :y y1 :id :start-deg :type :handle :tag :edit :element id}
           {:x x2 :y y2 :id :end-deg :type :handle :tag :edit :element id}])
     (map (fn [handle] [overlay/square-handle handle])
          [{:x (+ cx rx) :y cy :id :rx :type :handle :tag :edit :element id}
           {:x cx :y (- cy ry) :id :ry :type :handle :tag :edit :element id}])]))
