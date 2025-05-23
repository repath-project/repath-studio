(ns renderer.tool.handlers
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.handlers]
   [renderer.frame.db :refer [DomRect]]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.db :refer [Tool State Cursor]]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.element :as utils.element]
   [renderer.utils.math :refer [Vec2]]))

(m/=> add-fx [:-> App vector? App])
(defn add-fx
  [db effect]
  (update db :fx conj effect))

(m/=> set-state [:-> App State App])
(defn set-state
  [db state]
  (assoc db :state state))

(m/=> set-cursor [:-> App Cursor App])
(defn set-cursor
  [db cursor]
  (assoc db :cursor cursor))

(m/=> activate [:-> App Tool App])
(defn activate
  [db tool]
  (-> db
      (tool.hierarchy/on-deactivate)
      (assoc :tool tool)
      (set-state :idle)
      (set-cursor "default")
      (dissoc :drag :pointer-offset :clicked-element)
      (snap.handlers/rebuild-tree)
      (tool.hierarchy/on-activate)))

(m/=> pointer-delta [:-> App Vec2])
(defn pointer-delta
  [db]
  (matrix/sub (:adjusted-pointer-pos db) (:adjusted-pointer-offset db)))

(m/=> dissoc-temp [:-> App App])
(defn dissoc-temp
  [db]
  (cond-> db
    (:active-document db)
    (update-in [:documents (:active-document db)] dissoc :temp-element)))

(m/=> set-temp [:-> App map? App])
(defn set-temp
  [db el]
  (->> (utils.element/normalize-attrs el)
       (assoc-in db [:documents (:active-document db) :temp-element])))

(m/=> temp [:-> App [:maybe Element]])
(defn temp
  [db]
  (get-in db [:documents (:active-document db) :temp-element]))

(m/=> create-temp-element [:-> App App])
(defn create-temp-element
  [db]
  (->> (temp db)
       (element.handlers/add db)
       (dissoc-temp)))

(m/=> axis-pan-offset [:-> number? number? number? number?])
(defn axis-pan-offset
  [position offset size]
  (let [threshold 50
        step 15]
    (cond
      (and (< position threshold)
           (< position offset))
      (- step)

      (and (> position (- size threshold))
           (> position offset))
      step

      :else 0)))

(m/=> pan-out-of-canvas [:-> App DomRect Vec2 Vec2 App])
(defn pan-out-of-canvas
  [db dom-rect pointer-pos pointer-offset]
  (let [[x y] pointer-pos
        [offset-x offset-y] pointer-offset
        pan [(axis-pan-offset x offset-x (:width dom-rect))
             (axis-pan-offset y offset-y (:height dom-rect))]]
    (cond-> db
      (not-every? zero? pan)
      (-> (frame.handlers/pan-by pan)
          ; REVIEW: Can we improve performance?
          (snap.handlers/update-viewport-tree)))))

(m/=> cancel [:-> App App])
(defn cancel
  [db]
  (cond-> db
    :always
    (-> (activate (:tool db))
        (dissoc-temp)
        (history.handlers/reset-state))

    (= (:state db) :idle)
    (activate :transform)

    :always
    (set-state :idle)))
