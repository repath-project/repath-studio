(ns renderer.tool.impl.element.text
  (:require
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.app :as-alias app.effects]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :text ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :text
  []
  {:icon "text"})

(defmethod tool.hierarchy/help [:text :idle]
  []
  "Click to enter your text.")

(defmethod tool.hierarchy/on-activate :text
  [db]
  (tool.handlers/set-cursor db "text"))

(defmethod tool.hierarchy/on-pointer-up :text
  [db _e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        el {:type :element
            :tag :text
            :attrs {:x offset-x
                    :y offset-y}}]
    (-> (element.handlers/deselect-all db)
        (element.handlers/add el)
        (history.handlers/finalize "Create text")
        (tool.handlers/activate :edit)
        (tool.handlers/set-state :create))))

(defmethod tool.hierarchy/on-drag-end :text
  [db e]
  (tool.hierarchy/on-pointer-up db e))
