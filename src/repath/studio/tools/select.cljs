(ns repath.studio.tools.select
  (:require [repath.studio.tools.base :as tools]
            [clojure.core.matrix :as matrix]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.styles :as styles]
            [repath.studio.history.handlers :as history]))

(derive :select ::tools/transform)

(defmethod tools/properties :select [] {:icon "pointer"})

(defn hover-by-area
  [db intersecting?]
  (let [active-document (:active-document db)
        temp-element (get-in db [:documents (:active-document db) :temp-element])]
    (-> db
        (assoc-in [:documents (:active-document db) :hovered-keys] #{})
        (elements/conj-by-bounds-overlap (if intersecting? elements/bounds-intersect? elements/bounds-contained?) [:documents active-document :hovered-keys] temp-element))))

(defn select-by-area
  [db intersecting?]
  (let [active-document (:active-document db)
        temp-element (get-in db [:documents active-document :temp-element])]
    (-> db
        (elements/deselect-all)
        (elements/clear-temp)
        (elements/conj-by-bounds-overlap (if intersecting? elements/bounds-intersect? elements/bounds-contained?) [:documents active-document :selected-keys] temp-element))))

(defmethod tools/mouse-move :select
  [{active-document :active-document :as db} _ element tool-data]
  (-> db
      (assoc-in [:documents active-document :hovered-keys] (if element #{(:key element)} #{}))
      (assoc :cursor (if element "move" "default"))))

(defmethod tools/activate :select
  [db _ _]
  (assoc db :cursor "default"))

(defmethod tools/drag :select
  [{:keys [state adjusted-mouse-offset] :as db} event element {:keys [adjusted-mouse-pos zoom is-element-selected?]}]
  (if (or (and (not element) (= state :default)) (= state :select))
    (let [[offset-x offset-y] adjusted-mouse-offset
          [pos-x pos-y] adjusted-mouse-pos
          intersecting? (> pos-x offset-x)
          attrs {:key    :select
                 :x      (min pos-x offset-x)
                 :y      (min pos-y offset-y)
                 :width  (Math/abs (- pos-x offset-x))
                 :height (Math/abs (- pos-y offset-y))
                 :fill   (if intersecting? styles/accent "transparent")
                 :fill-opacity ".25"
                 :stroke styles/accent
                 :stroke-opacity ".5"
                 :stroke-width (/ 1 zoom)}]
      (-> db
          (assoc :state :select)
          (elements/set-temp {:type :rect :attrs attrs})
          (hover-by-area intersecting?)))
    (let [offset (matrix/sub adjusted-mouse-pos (:adjusted-mouse-pos db))]
      (case state
        :move (elements/move (if (some #(contains? (:modifiers event) %) #{:ctrl})
                               (-> db
                                   (assoc :state :clone)
                                   (assoc :cursor "copy")
                                   (elements/duplicate))
                               db) offset)
        :clone (elements/move db offset)
        :scale (elements/scale db offset)
        (cond-> db
          (not (or is-element-selected? (= (:type element) :scale-handler))) (elements/select (some #(contains? (:modifiers event) %) #{:shift}) element)
          (not= (:type element) :scale-handler) (assoc :cursor "move")
          (not= (:type element) :scale-handler) (assoc :state :move)
          (= (:type element) :scale-handler) (assoc :state :scale)
          (= (:type element) :scale-handler) (assoc :scale (:key element)))))))

(defmethod tools/drag-end :select
  [{:keys [state adjusted-mouse-offset] :as db} _ _ {:keys [adjusted-mouse-pos]}]
  (let [[offset-x _] adjusted-mouse-offset
        [pos-x _] adjusted-mouse-pos]
    (cond-> db
      (= state :select) (select-by-area (> pos-x offset-x))
      (= state :move) (history/finalize "Move selection")
      (= state :scale) (history/finalize "Scale selection")
      (= state :clone) (history/finalize "Duplicate selection to position")
      :always (assoc :cursor "default")
      :always (assoc :state :default))))
