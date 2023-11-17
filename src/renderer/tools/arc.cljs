(ns renderer.tools.arc
  (:require
   [renderer.tools.base :as tools]
   [renderer.element.handlers :as elements]
   [renderer.utils.mouse :as mouse]
   [renderer.overlay :as overlay]
   [renderer.attribute.hierarchy :as hierarchy]
   [clojure.core.matrix :as matrix]
   [renderer.utils.units :as units]
   [renderer.attribute.angle :as angle]
   ["svg-path-bbox" :as svg-path-bbox]
   [re-frame.core :as rf]
   [goog.math]))

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
  [{:keys [adjusted-mouse-offset active-document adjusted-mouse-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:cx offset-x
               :cy offset-y
               ::start-deg 90
               ::end-deg 180
               :fill fill
               :stroke stroke
               :rx (abs (- pos-x offset-x))
               :ry (abs (- pos-y offset-y))}]
    (elements/set-temp db {:type :element :tag :arc :attrs attrs})))

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
        x1 (+ cx (goog.math/angleDx start-deg rx))
        y1 (+ cy (goog.math/angleDy start-deg ry))
        x2 (+ cx (goog.math/angleDx end-deg rx))
        y2 (+ cy (goog.math/angleDy end-deg ry))
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
        mouse-handler #(mouse/event-handler % element)]
    [:path (merge {:d (tools/path element)
                   :on-pointer-up mouse-handler
                   :on-pointer-down mouse-handler
                   :on-pointer-move mouse-handler
                   :on-double-click mouse-handler}
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
        x1 (+ cx (goog.math/angleDx start-deg rx))
        y1 (+ cy (goog.math/angleDy start-deg ry))
        x2 (+ cx (goog.math/angleDx end-deg rx))
        y2 (+ cy (goog.math/angleDy end-deg ry))
        page-pos (mapv
                  units/unit->px
                  [(-> active-page :attrs :x) (-> active-page :attrs :y)])
        [[cx cy][x1 y1][x2 y2]] (matrix/add page-pos [[cx cy][x1 y1][x2 y2]])
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
     (map (fn [handler] [overlay/circle-handler handler])
          [{:x x1 :y y1 :key ::start-deg :type :handler :tag :edit :element key}
           {:x x2 :y y2 :key ::end-deg :type :handler :tag :edit :element key}])
     (map (fn [handler] [overlay/square-handler handler])
          [{:x (+ cx rx) :y cy :key :rx :type :handler :tag :edit :element key}
           {:x cx :y (- cy ry) :key :ry :type :handler :tag :edit :element key}])]))
