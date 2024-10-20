(ns renderer.handle.views
  (:require
   [clojure.core.matrix :as mat]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.handle.db :refer [Handle]]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.theme.db :as theme.db]
   [renderer.utils.bounds :as bounds :refer [Bounds]]
   [renderer.utils.hiccup :refer [Hiccup]]
   [renderer.utils.pointer :as pointer]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(m/=> circle [:-> Handle Hiccup Hiccup])
(defn circle
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
                    :on-pointer-move pointer-handler
                    :on-scroll pointer-handler}] children)))

(m/=> square [:-> Handle Hiccup Hiccup])
(defn square
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
                  :on-pointer-move pointer-handler
                  :on-scroll pointer-handler}] children)))

(defn scale
  [props]
  ^{:key (:id props)}
  [square (merge props {:type :handle
                        :action :scale})])

(m/=> wrapping-bounding-box [:-> Bounds Hiccup])
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

(m/=> min-bounds [:-> Bounds Hiccup])
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

(m/=> bounding-corners [:-> Bounds Hiccup])
(defn bounding-corners
  [bounds]
  (let [bounds (min-bounds bounds)
        [x1 y1 x2 y2] bounds
        [w h] (bounds/->dimensions bounds)]
    [:g {:key :bounding-corners}
     (map scale
          [{:x x1 :y y1 :id :top-left :cursor "nwse-resize"}
           {:x x2 :y y1 :id :top-right :cursor "nesw-resize"}
           {:x x1 :y y2 :id :bottom-left :cursor "nesw-resize"}
           {:x x2 :y y2 :id :bottom-right :cursor "nwse-resize"}
           {:x (+ x1 (/ w 2)) :y y1 :id :top-middle :cursor "ns-resize"}
           {:x x2 :y (+ y1 (/ h 2)) :id :middle-right :cursor "ew-resize"}
           {:x x1 :y (+ y1 (/ h 2)) :id :middle-left :cursor "ew-resize"}
           {:x (+ x1 (/ w 2)) :y y2 :id :bottom-middle :cursor "ns-resize"}])]))
