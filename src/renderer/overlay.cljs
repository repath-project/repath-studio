(ns renderer.overlay
  "Render functions for canvas overlay objects (select helpers etc)."
  (:require
   [clojure.core.matrix :as mat]
   [goog.math :as math]
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.mouse :as mouse]
   [renderer.utils.units :as units]))

;; The iframe is isolated so we don't have access to the css vars of the parent.
;; We are currently using hardcoded values, but we hould be able to set those 
;; vars in the nested document if we have to.
(def stroke "#000")
(def stroke-inverted "#fff")
(def accent "#e93976")

(def handler-size 10)
(def dash-size 5)

(defn point-of-interest
  "Simple dot used for debugging purposes."
  [[x y] & children]
  (let [zoom @(rf/subscribe [:document/zoom])]
    (into [:circle {:cx x
                    :cy y
                    :stroke-width 0
                    :fill accent
                    :r (/ 3 zoom)}] children)))

(defn circle-handler
  [{:keys [x y key] :as el} & children]
  (let [zoom @(rf/subscribe [:document/zoom])
        clicked-element @(rf/subscribe [:clicked-element])
        mouse-handler #(mouse/event-handler % el)]
    [:circle {:key key
              :cx x
              :cy y
              :stroke stroke
              :stroke-width (/ 1 zoom)
              :fill (if (= (:key clicked-element) key)
                      accent
                      stroke-inverted)
              :r (/ 4 zoom)
              :cursor "default"
              :on-pointer-up mouse-handler
              :on-pointer-down mouse-handler
              :on-pointer-move mouse-handler
              :on-scroll mouse-handler} children]))

(defn square-handler
  [{:keys [x y key cursor] :as el} & children]
  (let [zoom @(rf/subscribe [:document/zoom])
        state @(rf/subscribe [:state])
        clicked-element @(rf/subscribe [:clicked-element])
        hovered-keys @(rf/subscribe [:document/hovered-keys])
        size (/ handler-size zoom)
        stroke-width (/ 1 zoom)
        mouse-handler #(mouse/event-handler % el)
        clicked? (= (:key clicked-element) key)]
    (when (or clicked? (not= state :scale))
      [:rect {:key key
              :id (name key)
              :fill (if (or clicked? (contains? hovered-keys key))
                      accent
                      stroke-inverted)
              :stroke stroke
              :stroke-width stroke-width
              :x (- x (/ size 2))
              :y (- y (/ size 2))
              :shape-rendering "crispEdges"
              :width size
              :height size
              :cursor (if (or clicked? (not cursor)) "default" cursor)
              :on-pointer-up mouse-handler
              :on-pointer-down mouse-handler
              :on-pointer-move mouse-handler
              :on-scroll mouse-handler} children])))

(defn line
  ([x1 y1 x2 y2]
   (line x1 y1 x2 y2 true))
  ([x1 y1 x2 y2 dashed?]
   (let [zoom @(rf/subscribe [:document/zoom])
         stroke-width (/ 1 zoom)
         stroke-dasharray (/ 5 zoom)
         attrs {:x1 x1
                :y1 y1
                :x2 x2
                :y2 y2
                :stroke-width stroke-width
                :shape-rendering (when dashed? "crispEdges")}]
     [:g
      (when dashed? [:line (merge attrs {:stroke stroke-inverted})])
      [:line (merge attrs
                    {:stroke stroke
                     :stroke-dasharray (when dashed? stroke-dasharray)})]])))

(defn cross
  ([[x y]]
   (cross x y))
  ([x y]
   (let [zoom @(rf/subscribe [:document/zoom])
         size (/ handler-size zoom)]
     [:g
      [line (- x (/ size 2)) y (+ x (/ size 2)) y false]
      [line x (- y (/ size 2)) x (+ y (/ size 2)) false]])))

(defn arc
  [[x y] radius start-degrees size-degrees]
  (let [zoom @(rf/subscribe [:document/zoom])
        stroke-width (/ 1 zoom)
        radius (/ radius zoom)
        end-degrees (+ start-degrees size-degrees)
        stroke-dasharray (/ dash-size zoom)
        x1 (+ x (goog.math/angleDx start-degrees radius))
        y1 (+ y (goog.math/angleDy start-degrees radius))
        x2 (+ x (goog.math/angleDx end-degrees radius))
        y2 (+ y (goog.math/angleDy end-degrees radius))
        d (str "M" x1 "," y1 " "
               "A" radius "," radius " 0 0,1 " x2 "," y2)
        attrs {:d d
               :fill "transparent"
               :stroke-width stroke-width}]
    [:g
     [:path (merge {:stroke stroke-inverted} attrs)]
     [:path (merge {:stroke stroke
                    :stroke-dasharray stroke-dasharray} attrs)]]))

(defn times
  ([[x y]]
   (times x y))
  ([x y]
   (let [zoom @(rf/subscribe [:document/zoom])
         size (/ handler-size zoom)
         mid (/ size 2)]
     [:g
      [line
       (- x mid) (- y mid)
       (+ x mid) (+ y mid) false]
      [line
       (+ x mid) (- y mid)
       (- x mid) (+ y mid) false]])))

(defn scale-handler
  [attrs]
  [square-handler (merge attrs {:type :handler
                                :tag :scale})])

(defn min-bounds
  [bounds]
  (let [zoom @(rf/subscribe [:document/zoom])
        dimensions (bounds/->dimensions bounds)
        [w h] dimensions
        min-size (/ (* handler-size 2) zoom)]
    (cond-> bounds
      (< w min-size) (mat/add [(- (/ (- min-size w) 2)) 0
                               (/ (- min-size w) 2) 0])
      (< h min-size) (mat/add [0 (- (/ (- min-size h) 2))
                               0 (/ (- min-size h) 2)]))))

(defn bounding-handlers
  [bounds]
  (let [bounds (min-bounds bounds)
        [x1 y1 x2 y2] bounds
        [w h] (bounds/->dimensions bounds)]
    [:g {:key :bounding-handlers}
     (map (fn [handler] [scale-handler handler])
          [{:x x1 :y y1 :key :top-left :cursor "nwse-resize"}
           {:x x2 :y y1 :key :top-right :cursor "nesw-resize"}
           {:x x1 :y y2 :key :bottom-left :cursor "nesw-resize"}
           {:x x2 :y y2 :key :bottom-right :cursor "nwse-resize"}
           {:x (+ x1 (/ w 2)) :y y1 :key :top-middle :cursor "ns-resize"}
           {:x x2 :y (+ y1 (/ h 2)) :key :middle-right :cursor "ew-resize"}
           {:x x1 :y (+ y1 (/ h 2)) :key :middle-left :cursor "ew-resize"}
           {:x (+ x1 (/ w 2)) :y y2 :key :bottom-middle :cursor "ns-resize"}])]))

(defn label
  [text position anchor]
  (let [zoom @(rf/subscribe [:document/zoom])
        [x y] position
        font-size (/ 11 zoom)
        padding (/ 8 zoom)
        font-width (/ font-size 2)
        label-width (+ (* (count text) font-width)
                       font-size)
        label-height (+ font-size padding)
        text-anchor (or anchor "middle")]
    [:g
     [:rect {:x (case text-anchor
                  "start" (- x (/ padding 2))
                  "middle" (- x (/ label-width 2))
                  "end" (- x label-width (/ (- padding) 2)))
             :y (- y  (/ label-height 2))
             :fill "#eee"
             :rx (/ 4 zoom)
             :width label-width
             :height label-height} text]
     [:text {:x x
             :y y
             :fill "#555"
             :dominant-baseline "middle"
             :text-anchor text-anchor
             :width label-width
             :font-size font-size} text]]))

(defn size
  [bounds]
  (let [zoom @(rf/subscribe [:document/zoom])
        bounds (min-bounds bounds)
        [x1 _ x2 y2] bounds
        x (+ x1 (/ (- x2 x1) 2))
        y (+ y2 (/ (+ (/ handler-size 2) 15) zoom))
        [width height] (bounds/->dimensions bounds)
        text (str (units/->fixed width) " x " (units/->fixed height))]
    [label text [x y]]))

(defn bounding-box
  [bounds]
  (let [zoom @(rf/subscribe [:document/zoom])
        [x1 y1 _x2 _y2] bounds
        [width height] (bounds/->dimensions bounds)
        stroke-width (/ 1 zoom)
        stroke-dasharray (/ dash-size zoom)
        attrs {:x x1
               :y y1
               :width width
               :height height
               :stroke-width stroke-width
               :fill "transparent"}]

    [:g {:style {:pointer-events "none"}}
     [:rect (merge attrs {:stroke stroke-inverted})]
     [:rect (merge attrs {:stroke stroke
                          :stroke-dasharray stroke-dasharray})]]))

(defn select-box
  [adjusted-pointer-pos adjusted-pointer-offset zoom]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos]
    {:tag :rect :attrs {:x (min pos-x offset-x)
                        :y (min pos-y offset-y)
                        :width (abs (- pos-x offset-x))
                        :height (abs (- pos-y offset-y))
                        :shape-rendering "crispEdges"
                        :fill-opacity ".2"
                        :fill accent
                        :stroke accent
                        :stroke-opacity ".5"
                        :stroke-width (/ 1 zoom)}}))

(defn centroid
  [el]
  (when-let [centroid (tools/centroid el)]
    (let [active-page @(rf/subscribe [:element/active-page])
          page-pos (mapv units/unit->px [(-> active-page :attrs :x) (-> active-page :attrs :y)])
          centroid (if-not (= (:tag el) :page)
                     (mat/add page-pos centroid)
                     centroid)]
      [point-of-interest centroid

       [:title "Centroid"]])))

(defn area
  [area bounds]
  (when area
    (let [zoom @(rf/subscribe [:document/zoom])
          bounds (min-bounds bounds)
          [x1 y1 x2 _y2] bounds
          x (+ x1 (/ (- x2 x1) 2))
          y (+ y1 (/ (- -15 (/ handler-size 2)) zoom))
          text (str (units/->fixed area) " px²")]
      [label text [x y]])))
