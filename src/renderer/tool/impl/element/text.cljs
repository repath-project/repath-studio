(ns renderer.tool.impl.element.text
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h]
   [renderer.tool.app :as-alias app.fx]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :text ::hierarchy/tool)

(defmethod hierarchy/properties :text
  []
  {:icon "text"})

(defmethod hierarchy/help [:text :idle]
  []
  "Click to enter your text.")

(defmethod hierarchy/on-activate :text
  [db]
  (h/set-cursor db "text"))

(defmethod hierarchy/on-pointer-up :text
  [db _e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        el {:type :element
            :tag :text
            :attrs {:x offset-x
                    :y offset-y}}]
    (-> (element.h/deselect-all db)
        (element.h/add el)
        (history.h/finalize "Create text")
        (h/activate :edit)
        (h/set-state :create))))
