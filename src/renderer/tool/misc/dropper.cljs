(ns renderer.tool.misc.dropper
  (:require
   [renderer.app.handlers :as app.h]
   [renderer.color.effects :as-alias color.fx]
   [renderer.document.events :as-alias document.e]
   [renderer.element.events :as-alias element.e]
   [renderer.notification.events :as-alias notification.e]
   [renderer.tool.base :as tool]))

(derive :dropper ::tool/tool)

(defmethod tool/properties :dropper
  []
  {:icon "eye-dropper"
   :description "Pick a color from your document."})

(defmethod tool/activate :dropper
  [db]
  (-> db
      (app.h/set-message "Click anywhere to pick a color.")
      (app.h/add-fx [::color.fx/dropper {:on-success ::document.e/set-fill}])))
