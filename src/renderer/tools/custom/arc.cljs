(ns renderer.tools.custom.arc
  (:require
   ["svg-path-bbox" :as svg-path-bbox]
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.attribute.angle :as angle]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.element.handlers :as element.h]
   [renderer.tools.base :as tools]
   [renderer.tools.overlay :as overlay]
   [renderer.utils.math :as math]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :arc ::tools/custom)
(derive ::start-deg ::angle/angle)
(derive ::end-deg ::angle/angle)

(defmethod tools/properties :arc
  []
  {:icon "arc"
   :description "Draw arcs"
   :attrs [:cx
           :cy
           :rx
           :ry
           ::start-deg
           ::end-deg
           :stroke-width
           :opacity
           :fill
           :stroke
           :stroke-linecap
           :stroke-dasharray]})

(defmethod tools/drag :arc
  [{:keys [adjusted-pointer-offset active-document adjusted-pointer-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        attrs {:cx offset-x
               :cy offset-y
               ::start-deg 90
               ::end-deg 180
               :fill fill
               :stroke stroke
               :rx (abs (- pos-x offset-x))
               :ry (abs (- pos-y offset-y))}]
    (element.h/set-temp db {:type :element :tag :arc :attrs attrs})))

(defmethod tools/translate :arc
  [element [x y]] (-> element
                      (hierarchy/update-attr :cx + x)
                      (hierarchy/update-attr :cy + y)))

(defmethod tools/bounds :arc
  [element]
  (let [[left top right bottom] (js->clj (svg-path-bbox (tools/path element)))]
    [left top right bottom]))

(defmethod tools/area :arc
  [{{:keys [r]} :attrs}]
  (* Math/PI (Math/pow (units/unit->px r) 2)))

(defmethod tools/path :arc
  [{{:keys [cx cy rx ry ::start-deg ::end-deg]} :attrs}]
  (let [[cx cy rx ry] (map units/unit->px [cx cy rx ry])
        x1 (+ cx (math/angle-dx start-deg rx))
        y1 (+ cy (math/angle-dy start-deg ry))
        x2 (+ cx (math/angle-dx end-deg rx))
        y2 (+ cy (math/angle-dy end-deg ry))
        diff-deg (- end-deg start-deg)]
    (str "M" x1 "," y1 " "
         "A" rx "," ry " 0 " (if (>= diff-deg 180) 1 0) ",1 " x2 "," y2)))

(defmethod tools/edit :arc
  [element [x y] handler]
  (case handler
    :rx (hierarchy/update-attr element :rx #(abs (+ % x)))
    :ry (hierarchy/update-attr element :ry #(abs (- % y)))
    element))

(defmethod tools/render :arc
  [{:keys [attrs children] :as element}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        pointer-handler #(pointer/event-handler % element)]
    [:path (merge {:d (tools/path element)
                   :on-pointer-up pointer-handler
                   :on-pointer-down pointer-handler
                   :on-pointer-move pointer-handler
                   :on-double-click pointer-handler}
                  (select-keys attrs [:stroke-width
                                      :opacity
                                      :fill
                                      :stroke
                                      :stroke-linecap
                                      :stroke-dasharray])) child-elements]))

(defmethod tools/render-edit :arc
  [{:keys [attrs key]}]
  (let [{:keys [cx cy rx ry ::start-deg ::end-deg]} attrs
        [cx cy rx ry] (mapv units/unit->px [cx cy rx ry])
        active-page @(rf/subscribe [:element/active-page])
        x1 (+ cx (math/angle-dx start-deg rx))
        y1 (+ cy (math/angle-dy start-deg ry))
        x2 (+ cx (math/angle-dx end-deg rx))
        y2 (+ cy (math/angle-dy end-deg ry))
        page-pos (mapv
                  units/unit->px
                  [(-> active-page :attrs :x) (-> active-page :attrs :y)])
        [[cx cy] [x1 y1] [x2 y2]] (mat/add page-pos [[cx cy] [x1 y1] [x2 y2]])
        zoom @(rf/subscribe [:document/zoom])]
    [:g
     [overlay/times cx cy]
     [overlay/line cx cy (+ cx rx) cy]
     [overlay/label (str (units/->fixed rx)) [(+ cx (/ rx 2)) cy]]
     [overlay/line cx cy cx (- cy ry)]
     [overlay/label (str (units/->fixed ry)) [cx (- cy (/ ry 2))]]
     [:ellipse {:cx cx
                :cy cy
                :rx rx
                :ry ry
                :fill "transparent"
                :stroke-dasharray 2
                :stroke "#555"
                :stroke-width (/ 1 zoom)}]
     (map (fn [handle] [overlay/circle-handle handle])
          [{:x x1 :y y1 :key ::start-deg :type :handler :tag :edit :element key}
           {:x x2 :y y2 :key ::end-deg :type :handler :tag :edit :element key}])
     (map (fn [handle] [overlay/square-handle handle])
          [{:x (+ cx rx) :y cy :key :rx :type :handler :tag :edit :element key}
           {:x cx :y (- cy ry) :key :ry :type :handler :tag :edit :element key}])]))
