(ns renderer.tools.overlay
  "Render functions for canvas overlay objects (select helpers etc)."
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.math :as math]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

;; The iframe is isolated so we don't have access to the css vars of the parent.
;; We are currently using hardcoded values, but we hould be able to set those 
;; vars in the nested document if we have to.
(def accent-inverted "#fff")
(def accent "#e93976")
(def font-mono "'Consolas (Custom)', 'Bitstream Vera Sans Mono', monospace, 
                'Apple Color Emoji', 'Segoe UI Emoji'")

(def handler-size 12)
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
        pointer-handler #(pointer/event-handler % el)]
    [:circle {:key key
              :cx x
              :cy y
              :stroke accent
              :stroke-width (/ 1 zoom)
              :fill (if (= (:key clicked-element) key)
                      accent
                      accent-inverted)
              :r (/ 4 zoom)
              :cursor "default"
              :on-pointer-up pointer-handler
              :on-pointer-down pointer-handler
              :on-pointer-move pointer-handler
              :on-scroll pointer-handler} children]))

(defn square-handler
  [{:keys [x y key cursor] :as el} & children]
  (let [zoom @(rf/subscribe [:document/zoom])
        clicked-element @(rf/subscribe [:clicked-element])
        hovered-keys @(rf/subscribe [:document/hovered-keys])
        size (/ handler-size zoom)
        stroke-width (/ 1 zoom)
        pointer-handler #(pointer/event-handler % el)
        clicked? (= (:key clicked-element) key)
        active? (or clicked? (contains? hovered-keys key))]
    [:rect {:key key
            :id (name key)
            :fill (if active? accent accent-inverted)
            :stroke (if active? accent "#999")
            :stroke-width stroke-width
            :rx (/ 2 zoom)
            :x (- x (/ size 2))
            :y (- y (/ size 2))
            :width size
            :height size
            :cursor (if (or clicked? (not cursor)) "default" cursor)
            :on-pointer-up pointer-handler
            :on-pointer-down pointer-handler
            :on-pointer-move pointer-handler
            :on-scroll pointer-handler} children]))

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
      (when dashed? [:line (merge attrs {:stroke accent-inverted})])
      [:line (merge attrs
                    {:stroke accent
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
        x1 (+ x (math/angle-dx start-degrees radius))
        y1 (+ y (math/angle-dy start-degrees radius))
        x2 (+ x (math/angle-dx end-degrees radius))
        y2 (+ y (math/angle-dy end-degrees radius))
        d (str "M" x1 "," y1 " "
               "A" radius "," radius " 0 0,1 " x2 "," y2)
        attrs {:d d
               :fill "transparent"
               :stroke-width stroke-width}]
    [:g
     [:path (merge {:stroke accent-inverted} attrs)]
     [:path (merge {:stroke accent
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
(defn wrapping-bounding-box
  [bounds]
  (let [zoom @(rf/subscribe [:document/zoom])
        key :bounding-box
        ignored-keys @(rf/subscribe [:document/ignored-keys])
        ignored? (contains? ignored-keys key)
        [x1 y1 _x2 _y2] bounds
        [w h] (bounds/->dimensions bounds)
        pointer-handler #(pointer/event-handler % {:type :element
                                                   :tag :move
                                                   :key key})
        rect-attrs {:x x1
                    :y y1
                    :width w
                    :height h
                    :stroke-width (/ 2 zoom)
                    :stroke-opacity ".3"
                    :fill "transparent"
                    :shape-rendering "crispEdges"
                    :stroke accent
                    :pointer-events (when ignored? "none")}]
    [:rect (merge rect-attrs {:on-pointer-up pointer-handler
                              :on-pointer-down pointer-handler
                              :on-pointer-move pointer-handler})]))

(defn bounding-handlers
  [bounds]
  (let [bounds (min-bounds bounds)
        [x1 y1 x2 y2] bounds
        [w h] (bounds/->dimensions bounds)]
    [:g {:key :bounding-handlers}
     (map scale-handler
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
        font-size (/ 10 zoom)
        padding (/ 8 zoom)
        font-width (/ 6 zoom)
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
             :fill accent
             :rx (/ 4 zoom)
             :width label-width
             :height label-height} text]
     [:text {:x x
             :y y
             :fill accent-inverted
             :dominant-baseline "middle"
             :text-anchor text-anchor
             :width label-width
             :font-family font-mono
             :font-size font-size} text]]))

(defn size
  [bounds]
  (let [zoom @(rf/subscribe [:document/zoom])
        [x1 _ x2 y2] bounds
        x (+ x1 (/ (- x2 x1) 2))
        y (+ y2 (/ (+ (/ handler-size 2) 15) zoom))
        [width height] (bounds/->dimensions bounds)
        text (str (units/->fixed width) " x " (units/->fixed height))]
    [label text [x y]]))

(defn bounding-box
  [bounds dashed?]
  (let [zoom @(rf/subscribe [:document/zoom])
        [x1 y1 _x2 _y2] bounds
        [width height] (bounds/->dimensions bounds)
        stroke-width (/ 2 zoom)
        stroke-dasharray (/ dash-size zoom)
        attrs {:x x1
               :y y1
               :width width
               :height height
               :shape-rendering "crispEdges"
               :stroke-width stroke-width
               :fill "transparent"}]

    [:g {:style {:pointer-events "none"}}
     [:rect (merge attrs {:stroke accent})]
     (when dashed?
       [:rect (merge attrs {:stroke accent-inverted
                            :stroke-dasharray stroke-dasharray})])]))

(defn select-box
  [adjusted-pointer-pos adjusted-pointer-offset zoom]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos]
    {:tag :rect :attrs {:x (min pos-x offset-x)
                        :y (min pos-y offset-y)
                        :width (abs (- pos-x offset-x))
                        :height (abs (- pos-y offset-y))
                        :shape-rendering "crispEdges"
                        :fill-opacity ".1"
                        :fill accent
                        :stroke accent
                        :stroke-opacity ".5"
                        :stroke-width (/ 1 zoom)}}))

(defn centroid
  [el]
  (when-let [centroid (tools/centroid el)]
    (let [offset @(rf/subscribe [:element/el-offset el])
          centroid (mat/add offset centroid)]
      [point-of-interest centroid
       [:title "Centroid"]])))

(defn area
  [area bounds]
  (when area
    (let [zoom @(rf/subscribe [:document/zoom])
          [x1 y1 x2 _y2] bounds
          x (+ x1 (/ (- x2 x1) 2))
          y (+ y1 (/ (- -15 (/ handler-size 2)) zoom))
          text (str (units/->fixed area) " px²")]
      [label text [x y]])))
