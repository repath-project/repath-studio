(ns renderer.tool.impl.element.text
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :text ::hierarchy/element)

(defmethod hierarchy/properties :text
  []
  {:icon "text"})

(defmethod hierarchy/help [:text :idle]
  []
  "Click to enter your text.")

(defmethod hierarchy/activate :text
  [db]
  (h/set-cursor db "text"))

(defmethod hierarchy/pointer-up :text
  [db _e]
  (let [[offset-x offset-y] (:adjusted-pointer-offset db)
        el {:type :element
            :tag :text
            :attrs {:x offset-x
                    :y offset-y}}]
    (-> db
        (element.h/deselect)
        (element.h/add el)
        (h/activate :edit)
        (h/set-state :create))))

(defmethod hierarchy/drag-end :text
  [db e]
  (hierarchy/pointer-up db e))
