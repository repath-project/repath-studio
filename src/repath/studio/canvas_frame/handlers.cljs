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

(defn zoom-in-position
  [{active-document :active-document :as db} factor pos]
  (let [zoom (get-in db [:documents active-document :zoom])
        pan (get-in db [:documents active-document :pan])
        pan-updated (matrix/sub (matrix/div pan factor) (matrix/sub (matrix/div pos factor) pos))]
    (if (and (> (* zoom factor) 0.01) (< (* zoom factor) 100))
      (-> db
          (update-in [:documents active-document :zoom] * factor)
          (assoc-in [:documents active-document :pan] pan-updated))
      db)))

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

(defn pan-to-element
  [{active-document :active-document content-rect :content-rect :as db} key]
  (let [zoom (get-in db [:documents active-document :zoom])
        element (get-in db [:documents active-document :elements key])
        elements (el/elements db)
        [x1 y1 x2 y2] (tools/bounds elements element)
        [width height] (matrix/sub [x2 y2] [x1 y1])
        parrent-page-attrs (:attrs (helpers/parent-page elements element))
        pan (matrix/add
             (matrix/div (matrix/sub [width height] (matrix/div [(:width content-rect) (:height content-rect)] zoom)) 2)
             [x1 y1]
             (when-not (el/page? element) [(:x parrent-page-attrs) (:y parrent-page-attrs)]))]
    (assoc-in db [:documents active-document :pan] pan)))
