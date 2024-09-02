(ns renderer.tool.misc.fill
  (:require
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.base :as tool]))

(derive :fill ::tool/tool)

(defmethod tool/properties :fill
  []
  {:icon "fill"})

(defmethod tool/activate :fill
  [db]
  (-> db
      (assoc :cursor "crosshair")
      (app.h/set-message "Click on an element to fill.")))

(defmethod tool/translate :fill [])

(defmethod tool/pointer-up :fill
  [{active-document :active-document :as db} {:keys [element]}]
  (let [color (get-in db [:documents active-document :fill])]
    (-> db
        (element.h/set-attr (:id element) :fill color)
        (app.h/explain "Fill"))))

(defmethod tool/drag-end :fill
  [db e]
  (tool/pointer-up db e))
