(ns renderer.tool.misc.fill
  (:require
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :fill ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :fill
  []
  {:icon "fill"})

(defmethod tool.hierarchy/help [:fill :default]
  []
  "Click on an element to fill.")

(defmethod tool.hierarchy/activate :fill
  [db]
  (assoc db :cursor "crosshair"))

(defmethod tool.hierarchy/translate :fill [])

(defmethod tool.hierarchy/pointer-up :fill
  [{active-document :active-document :as db} {:keys [element]}]
  (let [color (get-in db [:documents active-document :fill])]
    (-> db
        (element.h/set-attr (:id element) :fill color)
        (app.h/explain "Fill"))))

(defmethod tool.hierarchy/drag-end :fill
  [db e]
  (tool.hierarchy/pointer-up db e))
