(ns repath.studio.tools.element
  (:require
   [repath.studio.tools.base :as tools]
   [repath.studio.elements.handlers :as elements]
   [repath.studio.history.handlers :as history]
   [repath.studio.units :as units]
   [clojure.core.matrix :as matrix]
   [clojure.string :as str]))

(defmethod tools/activate ::tools/element [db] (assoc db :cursor "crosshair"))

(defmethod tools/move ::tools/element
  [element [x y]] (-> element
                      (update-in [:attrs :x] (fn [val] (units/transform #(+ % x) val)))
                      (update-in [:attrs :y] (fn [val] (units/transform #(+ % y) val)))))

(defmethod tools/scale ::tools/element
  [element [x y] handler]
  (case handler
    :bottom-right (-> element
                      (update-in [:attrs :width] (fn [val] (units/transform #(+ % x) val)))
                      (update-in [:attrs :height] (fn [val] (units/transform #(+ % y) val))))
    :bottom-middle (update-in element [:attrs :height] (fn [val] (units/transform #(+ % y) val)))))

(defmethod tools/bounds ::tools/element
    [_ {:keys [attrs]}]
    (let [{:keys [x y width height stroke-width stroke]} attrs
          [x y width height stroke-width-px] (mapv units/unit->px [x y width height stroke-width])
          stroke-width-px (if (str/blank? stroke-width) 1 stroke-width-px)
          [x y] (matrix/sub [x y] (/ (if (str/blank? stroke) 0 stroke-width-px) 2))
          [width height] (matrix/add [width height] (if (str/blank? stroke) 0 stroke-width-px))]
      (mapv units/unit->px [x y (+ x width) (+ y height)])))

(defmethod tools/drag-end ::tools/element
  [db _ _]
  (let [temp-element (get-in db [:documents (:active-document db) :temp-element])]
    (if temp-element
      (-> db
          (elements/create temp-element)
          (elements/clear-temp)
          (history/finalize (str "Create " (name (:type temp-element)))))
      db)))

(defmethod tools/click :default
  [db event element tool-data]
  (elements/select db (some #(contains? (:modifiers event) %) #{:ctrl :shift}) element))