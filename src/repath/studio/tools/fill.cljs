(ns repath.studio.tools.fill
  (:require [repath.studio.tools.base :as tools]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.history.handlers :as history]))

  (derive :fill ::tools/edit)

  (defmethod tools/properties :fill [] {:icon "fill"})

  (defmethod tools/activate :fill
    [db]
    (assoc db :cursor "crosshair"))

  (defmethod tools/translate :fill [])

  (defmethod tools/mouse-up :fill
    [{active-document :active-document :as db} _ element]
    (let [color (tools/rgba (get-in db [:documents active-document :fill]))]
      (-> db
          (elements/set-attribute (:key element) :fill color)
          (history/finalize (str "Fill " color)))))
  
  (defmethod tools/drag-end :fill
    [db event element]
    (tools/mouse-up db event element))