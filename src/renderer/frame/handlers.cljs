(ns renderer.frame.handlers
  (:require
   [clojure.core.matrix :as mat]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.document.db :refer [ZoomFactor]]
   [renderer.element.handlers :as element.h]
   [renderer.frame.db :refer [DomRect Viewbox FocusType]]
   [renderer.utils.bounds :as utils.bounds :refer [BBox]]
   [renderer.utils.element :as element]
   [renderer.utils.math :as math :refer [Vec2]]
   [renderer.utils.pointer :as pointer]))

(m/=> viewbox [:function
               [:-> App [:maybe Viewbox]]
               [:-> ZoomFactor Vec2 DomRect Viewbox]])
(defn viewbox
  ([db]
   (let [zoom (get-in db [:documents (:active-document db) :zoom])
         pan (get-in db [:documents (:active-document db) :pan])]
     (when-let [dom-rect (:dom-rect db)]
       (viewbox zoom pan dom-rect))))
  ([zoom pan dom-rect]
   (let [{:keys [width height]} dom-rect
         [x y] pan
         [w h] (mat/div [width height] zoom)]
     [x y w h])))

(m/=> pan-by [:function
              [:-> App Vec2 App]
              [:-> App Vec2 uuid? App]])
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

(m/=> zoom-at-position [:-> App number? Vec2 App])
(defn zoom-at-position
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

(m/=> adjusted-pointer-pos [:-> App Vec2 Vec2])
(defn adjusted-pointer-pos
  [db pos]
  (let [{:keys [zoom pan]} (get-in db [:documents (:active-document db)])]
    (pointer/adjusted-position zoom pan pos)))

(m/=> zoom-at-pointer [:-> App number? App])
(defn zoom-at-pointer
  [db factor]
  (zoom-at-position db factor (:adjusted-pointer-pos db)))

(m/=> zoom-in-place [:-> App number? App])
(defn zoom-in-place
  [db factor]
  (let [{:keys [zoom pan]} (get-in db [:documents (:active-document db)])
        {:keys [width height]} (:dom-rect db)
        position (mat/add pan (mat/div [width height] 2 zoom))]
    (zoom-at-position db factor position)))

(m/=> pan-to-bbox [:-> App BBox App])
(defn pan-to-bbox
  [db bbox]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])
        rect-dimensions [(-> db :dom-rect :width) (-> db :dom-rect :height)]
        [min-x min-y] bbox
        pan (-> (utils.bounds/->dimensions bbox)
                (mat/sub (mat/div rect-dimensions zoom))
                (mat/div 2)
                (mat/add [min-x min-y]))]
    (assoc-in db [:documents (:active-document db) :pan] pan)))

(m/=> focus-bbox [:function
                  [:-> App FocusType App]
                  [:-> App FocusType BBox App]])
(defn focus-bbox
  ([db focus-type]
   (cond-> db
     (:active-document db)
     (focus-bbox
      focus-type
      (or (element.h/bbox db)
          (element/united-bbox (element.h/root-children db))))))
  ([db focus-type bbox]
   (let [[w h] (utils.bounds/->dimensions bbox)
         width-ratio (/ (-> db :dom-rect :width) w)
         height-ratio (/ (-> db :dom-rect :height) h)
         min-zoom (min width-ratio height-ratio)]
     (-> db
         (assoc-in [:documents (:active-document db) :zoom]
                   (case focus-type
                     :original (min (* min-zoom 0.9) 1)
                     :fit min-zoom
                     :fill (max width-ratio height-ratio)))
         (pan-to-bbox bbox)))))
