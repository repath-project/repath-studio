(ns renderer.tool.misc.dropper
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.app.handlers :as app.h]
   [renderer.color.effects :as-alias color.fx]
   [renderer.document.events :as-alias document.e]
   [renderer.element.events :as-alias element.e]
   [renderer.notification.events :as-alias notification.e]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.tool.base :as tool]))

(derive :dropper ::tool/tool)

(defmethod tool/properties :dropper
  []
  {:icon "eye-dropper"
   :description "Pick a color from your document."})

(defmethod tool/activate :dropper
  [db]
  (if (.-EyeDropper js/window)
    (-> db
        (app.h/set-message "Click anywhere to pick a color.")
        (app.h/add-fx [::color.fx/dropper {:on-success ::set-fill-and-deactivate}]))
    (-> db
        (notification.h/add [notification.v/unavailable-feature "EyeDropper" "https://developer.mozilla.org/en-US/docs/Web/API/EyeDropper_API#browser_compatibility"])
        (app.h/set-tool :select))))

(rf/reg-event-fx
 ::set-fill-and-deactivate
 (fn [_ [_ color]]
   {:fx [[:dispatch [::document.e/set-fill color]]
         [:dispatch [::app.e/set-tool :select]]]}))
