(ns renderer.tool.impl.element.element
  (:require
   [renderer.app.handlers :as app.h]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as element.h]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive ::tool.hierarchy/element ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [::tool.hierarchy/element :default]
  []
  "Click and drag to create an element.")

(defmethod tool.hierarchy/activate ::tool.hierarchy/element
  [db]
  (-> db
      (app.h/set-cursor "crosshair")
      (dissoc :drag :pointer-offset :clicked-element)
      (element.h/dissoc-temp)))

(defmethod tool.hierarchy/drag-start ::tool.hierarchy/element
  [db]
  (app.h/set-state db :create))

(defmethod tool.hierarchy/drag-end ::tool.hierarchy/element
  [db _e]
  (-> db
      (element.h/add)
      (app.h/set-tool :select)
      (app.h/set-state :default)
      (app.h/explain "Create " (name (:tag (element.h/get-temp db))))))
