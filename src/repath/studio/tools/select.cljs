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
  [db intersecting? multiselect?]
  (let [active-document (:active-document db)
        temp-element (get-in db [:documents active-document :temp-element])]
    (cond-> db
        (not multiselect?) (elements/deselect-all)
        :always (-> (elements/clear-temp)
                    (elements/conj-by-bounds-overlap (if intersecting? elements/bounds-intersect? elements/bounds-contained?) [:documents active-document :selected-keys] temp-element)))))

(defmethod tools/mouse-move :select
  [{active-document :active-document :as db} _ element]
  (-> db
      (assoc-in [:documents active-document :hovered-keys] (if element #{(:key element)} #{}))
      (assoc :cursor (if element "move" "default"))))

(defmethod tools/activate :select
  [db _ _]
  (assoc db :cursor "default"))

(defmethod tools/drag :select
  [{:keys [state adjusted-mouse-offset adjusted-mouse-pos active-document] :as db} event element]
  (if (or (and (not element) (= state :default)) (= state :select))
    (let [zoom (get-in db [:documents active-document :zoom])
          [offset-x offset-y] adjusted-mouse-offset
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
    (let [selected-keys (get-in db [:documents active-document :selected-keys])
          offset (:adjusted-mouse-diff db)
          is-element-selected? (contains? selected-keys (:key element))]
      (case state
        :translate (elements/translate (if (some #(contains? (:modifiers event) %) #{:ctrl})
                                         (-> db
                                             (assoc :state :clone
                                                    :cursor "copy")
                                             (elements/duplicate))
                                         db) offset)
        :clone (elements/translate db offset)
        :scale (elements/scale db offset)
        (cond-> db
          (not (or is-element-selected? (= (:type element) :scale-handler))) (elements/select (some #(contains? (:modifiers event) %) #{:shift}) element)
          (not= (:type element) :scale-handler) (assoc :cursor "move"
                                                       :state :translate)
          (= (:type element) :scale-handler) (assoc :state :scale
                                                    :cursor (:cursor element)
                                                    :scale (:key element)))))))

(defmethod tools/drag-end :select
  [{:keys [state adjusted-mouse-offset adjusted-mouse-pos] :as db} event]
  (let [[offset-x _] adjusted-mouse-offset
        [pos-x _] adjusted-mouse-pos]
    (assoc (case state
             :select (select-by-area db (> pos-x offset-x) (some #(contains? (:modifiers event) %) #{:ctrl :shift}))
             :translate (history/finalize db "Move selection")
             :scale (history/finalize db "Scale selection")
             :clone (history/finalize db "Duplicate selection to position"))
           :cursor "default"
           :state :default)))
