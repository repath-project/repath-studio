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
      (handlers/set-message "Click on an element to fill.")))

(defmethod tool/translate :fill [])

(defmethod tool/pointer-up :fill
  [{active-document :active-document :as db} {:keys [element]}]
  (let [color (get-in db [:documents active-document :fill])]
    (-> db
        (element.h/set-attr (:key element) :fill color)
        (history/finalize "Fill " color))))

(defmethod tool/drag-end :fill
  [db e]
  (tool/pointer-up db e))
