(ns renderer.tool.views
  (:require
   [clojure.core.matrix :as mat]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.theme.db :as theme.db]
   [renderer.utils.bounds :as bounds :refer [Bounds]]
   [renderer.utils.pointer :as pointer]))

#_(defn circle-handle
    [el & children]
    (let [{:keys [x y id]} el
          zoom @(rf/subscribe [::document.s/zoom])
          clicked-element @(rf/subscribe [::app.s/clicked-element])
          pointer-handler #(pointer/event-handler! % el)]
      (into [:circle {:key id
                      :cx x
                      :cy y
                      :stroke theme.db/accent
                      :stroke-width (/ 1 zoom)
                      :fill (if (= (:key clicked-element) id)
                              theme.db/accent
                              theme.db/accent-inverted)
                      :r (/ 4 zoom)
                      :cursor "default"
                      :on-pointer-up pointer-handler
                      :on-pointer-down pointer-handler
                      :on-pointer-move pointer-handler}] children)))

(defn square-handle
  [el & children]
  (let [{:keys [x y id cursor element]} el
        zoom @(rf/subscribe [::document.s/zoom])
        clicked-element @(rf/subscribe [::app.s/clicked-element])
        size (/ theme.db/handle-size zoom)
        stroke-width (/ 1 zoom)
        pointer-handler #(pointer/event-handler! % el)
        active (and (= (:id clicked-element) id)
                    (= (:element clicked-element) element))]
    (into [:rect {:fill (if active theme.db/accent theme.db/accent-inverted)
                  :stroke (if active theme.db/accent "#777")
                  :stroke-width stroke-width
                  :x (- x (/ size 2))
                  :y (- y (/ size 2))
                  :width size
                  :height size
                  :cursor (if (or active (not cursor)) "default" cursor)
                  :on-pointer-up pointer-handler
                  :on-pointer-down pointer-handler
                  :on-pointer-move pointer-handler}] children)))

(defn scale-handle
  [props]
  ^{:key (:id props)}
  [square-handle (merge props {:type :handle
                               :action :scale})])

(defn wrapping-bounding-box
  [bounds]
  (let [zoom @(rf/subscribe [::document.s/zoom])
        id :bounding-box
        ignored-ids @(rf/subscribe [::document.s/ignored-ids])
        ignored? (contains? ignored-ids id)
        [x1 y1 _x2 _y2] bounds
        [w h] (bounds/->dimensions bounds)
        pointer-handler #(pointer/event-handler! % {:type :handle
                                                    :action :translate
                                                    :id id})
        rect-attrs {:x x1
                    :y y1
                    :width w
                    :height h
                    :stroke-width (/ 2 zoom)
                    :stroke-opacity ".3"
                    :fill "transparent"
                    :shape-rendering "crispEdges"
                    :stroke theme.db/accent
                    :pointer-events (when ignored? "none")}]
    [:rect (merge rect-attrs {:on-pointer-up pointer-handler
                              :on-pointer-down pointer-handler
                              :on-pointer-move pointer-handler})]))

(m/=> min-bounds [:-> Bounds Bounds])
(defn min-bounds
  [bounds]
  (let [zoom @(rf/subscribe [::document.s/zoom])
        dimensions (bounds/->dimensions bounds)
        [w h] dimensions
        min-size (/ (* theme.db/handle-size 2) zoom)]
    (cond-> bounds
      (< w min-size) (mat/add [(- (/ (- min-size w) 2)) 0
                               (/ (- min-size w) 2) 0])
      (< h min-size) (mat/add [0 (- (/ (- min-size h) 2))
                               0 (/ (- min-size h) 2)]))))

(defn bounding-corners
  [bounds]
  (let [bounds (min-bounds bounds)
        [x1 y1 x2 y2] bounds
        [w h] (bounds/->dimensions bounds)]
    [:g {:key :bounding-corners}
     (map scale-handle
          [{:x x1 :y y1 :id :top-left :cursor "nwse-resize"}
           {:x x2 :y y1 :id :top-right :cursor "nesw-resize"}
           {:x x1 :y y2 :id :bottom-left :cursor "nesw-resize"}
           {:x x2 :y y2 :id :bottom-right :cursor "nwse-resize"}
           {:x (+ x1 (/ w 2)) :y y1 :id :top-middle :cursor "ns-resize"}
           {:x x2 :y (+ y1 (/ h 2)) :id :middle-right :cursor "ew-resize"}
           {:x x1 :y (+ y1 (/ h 2)) :id :middle-left :cursor "ew-resize"}
           {:x (+ x1 (/ w 2)) :y y2 :id :bottom-middle :cursor "ns-resize"}])]))
