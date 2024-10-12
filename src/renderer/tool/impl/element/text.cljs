(ns renderer.tool.impl.element.text
  (:require
   [renderer.app.events :as-alias app.e]
   [renderer.app.handlers :as app.h]
   [renderer.element.events :as-alias element.e]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :text ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :text
  []
  {:icon "text"})

(defmethod tool.hierarchy/help [:text :default]
  []
  "Click to enter your text.")

(defmethod tool.hierarchy/activate :text
  [db]
  (app.h/set-cursor db "text"))

(defmethod tool.hierarchy/pointer-up :text
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

(defmethod tool.hierarchy/drag-end :text
  [db e]
  (tool.hierarchy/pointer-up db e))
