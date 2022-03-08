(ns repath.studio.canvas-frame.handlers
  (:require
   [repath.studio.tools.base :as tools]
   [repath.studio.elements.handlers :as el]
   [repath.studio.helpers :as helpers]
   [clojure.core.matrix :as matrix]))

(defn pan
  [{active-document :active-document :as db} offset]
  (let [zoom (get-in db [:documents active-document :zoom])]
    (update-in db [:documents active-document :pan] matrix/add (matrix/div offset zoom))))

(defn validate-zoom
  [zoom]
  (cond
    (< zoom 0.01) 0.01
    (> zoom 100) 100
    :else zoom))

(defn zoom-in-position
  [{active-document :active-document :as db} factor pos]
  (let [zoom (get-in db [:documents active-document :zoom])
        updated-zoom (validate-zoom (* zoom factor))
        updated-factor (/ updated-zoom zoom)
        pan (get-in db [:documents active-document :pan])
        updated-pan (matrix/sub (matrix/div pan updated-factor) (matrix/sub (matrix/div pos updated-factor) pos))]
    (-> db
        (assoc-in [:documents active-document :zoom] updated-zoom)
        (assoc-in [:documents active-document :pan] updated-pan))))

(defn adjust-mouse-pos
  [zoom pan mouse-pos]
  (-> mouse-pos
      (matrix/div zoom)
      (matrix/add pan)))

(defn adjusted-mouse-pos
  [{:keys [active-document] :as db} mouse-pos]
  (let [{:keys [zoom pan]} (get-in db [:documents active-document])]
    (adjust-mouse-pos zoom pan mouse-pos)))

(defn zoom-in-mouse-position
  [{:keys [mouse-pos] :as db} factor]
    (zoom-in-position db factor (adjusted-mouse-pos db mouse-pos)))

(defn zoom
  [{active-document :active-document content-rect :content-rect :as db} factor]
  (let [{:keys [zoom pan]} (get-in db [:documents active-document])
        {:keys [width height]} content-rect
        position (matrix/add pan (matrix/div [width height] 2 zoom))]
    (zoom-in-position db factor position)))

(defn pan-to-bounds
  [{active-document :active-document content-rect :content-rect :as db} bounds]
  (let [zoom (get-in db [:documents active-document :zoom])
        [x1 y1 x2 y2] bounds
        [width height] (matrix/sub [x2 y2] [x1 y1])
        pan (matrix/add
             (matrix/div (matrix/sub [width height] (matrix/div [(:width content-rect) (:height content-rect)] zoom)) 2)
             [x1 y1])]
    (assoc-in db [:documents active-document :pan] pan)))

(defn pan-to-element
  [{active-document :active-document :as db} key]
  (let [element (get-in db [:documents active-document :elements key])
        elements (el/elements db)
        parrent-page-attrs (:attrs (helpers/parent-page elements element))
        db (pan-to-bounds db (tools/bounds elements element))]
    (if (not (el/page? element))
      (pan db [(:x parrent-page-attrs) (:y parrent-page-attrs)])
      db)))
