(ns renderer.tool.views
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.event.impl.pointer :as event.impl.pointer]
   [renderer.theme.db :as theme.db]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.bounds :as utils.bounds :refer [BBox]]))

#_(defn circle-handle
    [el & children]
    (let [{:keys [x y id]} el
          zoom @(rf/subscribe [::document.subs/zoom])
          clicked-element @(rf/subscribe [::app.subs/clicked-element])
          pointer-handler #(event.pointer/handler! % el)]
      (into [:circle {:key id
                      :cx x
                      :cy y
                      :stroke "var(--color-accent)"
                      :stroke-width (/ 1 zoom)
                      :fill (if (= (:key clicked-element) id)
                              "var(--color-accent)"
                              "var(--color-accent-inverted)")
                      :r (/ 4 zoom)
                      :cursor "default"
                      :on-pointer-up pointer-handler
                      :on-pointer-down pointer-handler
                      :on-pointer-move pointer-handler}] children)))

(defn square-handle
  [el & children]
  (let [{:keys [x y id cursor element]} el
        zoom @(rf/subscribe [::document.subs/zoom])
        clicked-element @(rf/subscribe [::app.subs/clicked-element])
        size (/ theme.db/handle-size zoom)
        stroke-width (/ 1 zoom)
        pointer-handler #(event.impl.pointer/handler! % el)
        active (and (= (:id clicked-element) id)
                    (= (:element clicked-element) element))]
    (into [:rect {:fill (if active "var(--color-accent)" "var(--color-accent-inverted)")
                  :stroke (if active "var(--color-accent)" "gray")
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

(defn wrapping-bbox
  [bbox]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        id :bbox
        ignored-ids @(rf/subscribe [::document.subs/ignored-ids])
        ignored? (contains? ignored-ids id)
        [min-x min-y] bbox
        [w h] (utils.bounds/->dimensions bbox)
        pointer-handler #(event.impl.pointer/handler! % {:type :handle
                                                         :action :translate
                                                         :id id})
        rect-attrs {:x min-x
                    :y min-y
                    :width w
                    :height h
                    :stroke-width (/ 2 zoom)
                    :stroke-opacity ".3"
                    :fill "transparent"
                    :shape-rendering "crispEdges"
                    :stroke "var(--color-accent)"
                    :pointer-events (when ignored? "none")}]
    [:rect (merge rect-attrs {:on-pointer-up pointer-handler
                              :on-pointer-down pointer-handler
                              :on-pointer-move pointer-handler})]))

(m/=> min-bbox [:-> BBox BBox])
(defn min-bbox
  "Ensures the bounding box is large enough to avoid overlapping handles."
  [bbox]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        dimensions (utils.bounds/->dimensions bbox)
        [w h] dimensions
        min-size (/ (* theme.db/handle-size 2) zoom)]
    (cond-> bbox
      (< w min-size)
      (matrix/add [(- (/ (- min-size w) 2)) 0
                   (/ (- min-size w) 2) 0])

      (< h min-size)
      (matrix/add [0 (- (/ (- min-size h) 2))
                   0 (/ (- min-size h) 2)]))))

(defn bounding-corners
  [bbox]
  (let [state @(rf/subscribe [::tool.subs/state])
        clicked-element @(rf/subscribe [::app.subs/clicked-element])
        bbox (min-bbox bbox)
        [min-x min-y max-x max-y] bbox
        [w h] (utils.bounds/->dimensions bbox)]
    [:g {:key :bounding-corners}
     (->> [{:x min-x :y min-y :id :top-left :cursor "nwse-resize"}
           {:x max-x :y min-y :id :top-right :cursor "nesw-resize"}
           {:x min-x :y max-y :id :bottom-left :cursor "nesw-resize"}
           {:x max-x :y max-y :id :bottom-right :cursor "nwse-resize"}
           {:x (+ min-x (/ w 2)) :y min-y :id :top-middle :cursor "ns-resize"}
           {:x max-x :y (+ min-y (/ h 2)) :id :middle-right :cursor "ew-resize"}
           {:x min-x :y (+ min-y (/ h 2)) :id :middle-left :cursor "ew-resize"}
           {:x (+ min-x (/ w 2)) :y max-y :id :bottom-middle :cursor "ns-resize"}]
          (map #(when (or (= state :idle)
                          (and (= state :scale)
                               (= (:id %) (:id clicked-element))))
                  ^{:key (:id %)}
                  [scale-handle %])))]))
