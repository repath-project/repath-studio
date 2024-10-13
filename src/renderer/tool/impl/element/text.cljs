(ns renderer.tool.impl.element.text
  (:require
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :text ::hierarchy/element)

(defmethod hierarchy/properties :text
  []
  {:icon "text"})

(defmethod hierarchy/help [:text :default]
  []
  "Click to enter your text.")

(defmethod hierarchy/activate :text
  [db]
  (app.h/set-cursor db "text"))

(defmethod hierarchy/pointer-up :text
  [{:keys [adjusted-pointer-offset] :as db} _e]
  (let [[offset-x offset-y] adjusted-pointer-offset
        attrs {:x offset-x
               :y offset-y}]
    (-> db
        (element.h/deselect)
        (element.h/add {:type :element
                        :tag :text
                        :attrs attrs})
        (app.h/set-tool :edit)
        (app.h/set-state :create))))

(defmethod hierarchy/drag-end :text
  [db e]
  (hierarchy/pointer-up db e))
