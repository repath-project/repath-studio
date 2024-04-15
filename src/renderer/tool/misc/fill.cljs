(ns renderer.tool.misc.fill
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as handlers]
   [renderer.history.handlers :as history]
   [renderer.tool.base :as tool]))

(derive :fill ::tool/tool)

(defmethod tool/properties :fill
  []
  {:icon "fill"})

(defmethod tool/activate :fill
  [db]
  (-> db
      (assoc :cursor "crosshair")
      (handlers/set-message  [:div "Click on an element to fill."])))

(defmethod tool/translate :fill [])

(defmethod tool/pointer-up :fill
  [{active-document :active-document :as db} _e el]
  (let [color (get-in db [:documents active-document :fill])]
    (-> db
        (element.h/set-attr (:key el) :fill color)
        (history/finalize "Fill " color))))

(defmethod tool/drag-end :fill
  [db e el]
  (tool/pointer-up db e el))
