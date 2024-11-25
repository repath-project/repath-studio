(ns renderer.frame.handlers
  (:require
   [clojure.core.matrix :as mat]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.document.db :refer [ZoomFactor]]
   [renderer.element.handlers :as element.h]
   [renderer.frame.db :refer [DomRect Viewbox FocusType]]
   [renderer.utils.bounds :as utils.bounds :refer [Bounds]]
   [renderer.utils.element :as element]
   [renderer.utils.math :as math :refer [Vec2D]]
   [renderer.utils.pointer :as pointer]))

(m/=> viewbox [:function
               [:-> App [:maybe Viewbox]]
               [:-> ZoomFactor Vec2D DomRect Viewbox]])
(defn viewbox
  ([db]
   (let [zoom (get-in db [:documents (:active-document db) :zoom])
         pan (get-in db [:documents (:active-document db) :pan])]
     (when-let [dom-rect (:dom-rect db)]
       (viewbox zoom pan dom-rect))))
  ([zoom pan dom-rect]
   (let [{:keys [width height]} dom-rect
         [x y] pan
         [width height] (mat/div [width height] zoom)]
     [x y width height])))

(m/=> pan-by [:function
              [:-> App Vec2D App]
              [:-> App Vec2D uuid? App]])
(defn pan-by
  ([db offset]
   (pan-by db offset (:active-document db)))
  ([db offset id]
   (let [zoom (get-in db [:documents id :zoom])]
     (update-in db [:documents id :pan] mat/add (mat/div offset zoom)))))

(m/=> recenter-to-dom-rect [:-> App DomRect App])
(defn recenter-to-dom-rect
  [db updated-dom-rect]
  (let [offset (-> (merge-with - (:dom-rect db) updated-dom-rect)
                   (select-keys [:width :height]))]
    (if-not (-> db :window :focused)
      db
      (->> (:document-tabs db)
           (reduce #(pan-by %1 (mat/div [(:width offset) (:height offset)] 2) %2) db)))))

(m/=> zoom-in-place [:-> App number? Vec2D App])
(defn zoom-in-place
  [db factor pos]
  (let [active-document (:active-document db)
        zoom (get-in db [:documents active-document :zoom])
        updated-zoom (math/clamp (* zoom factor) 0.01 100)
        updated-factor (/ updated-zoom zoom)
        current-pan (get-in db [:documents active-document :pan])
        updated-pan (mat/sub (mat/div current-pan updated-factor)
                             (mat/sub (mat/div pos updated-factor)
                                      pos))]
    (-> db
        (assoc-in [:documents active-document :zoom] updated-zoom)
        (assoc-in [:documents active-document :pan] updated-pan))))

(m/=> adjusted-pointer-pos [:-> App Vec2D Vec2D])
(defn adjusted-pointer-pos
  [db pos]
  (let [{:keys [zoom pan]} (get-in db [:documents (:active-document db)])]
    (pointer/adjusted-position zoom pan pos)))

(m/=> zoom-at-pointer [:-> App number? App])
(defn zoom-at-pointer
  [db factor]
  (zoom-in-place db factor (:adjusted-pointer-pos db)))

(m/=> zoom-by [:-> App number? App])
(defn zoom-by
  [db factor]
  (let [{:keys [zoom pan]} (get-in db [:documents (:active-document db)])
        {:keys [width height]} (:dom-rect db)
        position (mat/add pan (mat/div [width height] 2 zoom))]
    (zoom-in-place db factor position)))

(m/=> pan-to-bounds [:-> App Bounds App])
(defn pan-to-bounds
  [db bounds]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])
        rect-dimensions [(-> db :dom-rect :width) (-> db :dom-rect :height)]
        [x1 y1] bounds
        pan (-> (utils.bounds/->dimensions bounds)
                (mat/sub (mat/div rect-dimensions zoom))
                (mat/div 2)
                (mat/add [x1 y1]))]
    (assoc-in db [:documents (:active-document db) :pan] pan)))

(m/=> focus-bounds [:function
                    [:-> App FocusType App]
                    [:-> App FocusType Bounds App]])
(defn focus-bounds
  ([db focus-type]
   (cond-> db
     (:active-document db)
     (focus-bounds
      focus-type
      (or (element.h/bounds db)
          (element/united-bounds (element.h/root-children db))))))
  ([db focus-type bounds]
   (let [[width height] (utils.bounds/->dimensions bounds)
         width-ratio (/ (-> db :dom-rect :width) width)
         height-ratio (/ (-> db :dom-rect :height) height)
         min-zoom (min width-ratio height-ratio)]
     (-> db
         (assoc-in [:documents (:active-document db) :zoom]
                   (case focus-type
                     :original (min (* min-zoom 0.9) 1)
                     :fit min-zoom
                     :fill (max width-ratio height-ratio)))
         (pan-to-bounds bounds)))))
