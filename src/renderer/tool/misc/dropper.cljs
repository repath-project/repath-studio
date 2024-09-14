(ns renderer.tool.misc.dropper
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.app.handlers :as app.h]
   [renderer.color.effects :as-alias color.fx]
   [renderer.document.events :as-alias document.e]
   [renderer.notification.events :as-alias notification.e]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :dropper ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :dropper
  []
  {:icon "eye-dropper"
   :description "Pick a color from your document."})

(defmethod tool.hierarchy/help [:dropper :default]
  []
  "Click anywhere to pick a color.")

(defmethod tool.hierarchy/activate :dropper
  [db]
  (if (.-EyeDropper js/window)
    (app.h/add-fx db [::color.fx/dropper {:on-success ::set-fill-and-deactivate
                                          :on-error ::display-error-and-deactivate}])
    (-> db
        (app.h/set-tool :select)
        (notification.h/add
         [notification.v/unavailable-feature
          "EyeDropper"
          "https://developer.mozilla.org/en-US/docs/Web/API/EyeDropper_API#browser_compatibility"]))))

(rf/reg-event-fx
 ::set-fill-and-deactivate
 (fn [_ [_ ^js color]]
   {:fx [[:dispatch [::document.e/set-fill (.-sRGBHex color)]]
         [:dispatch [::app.e/set-tool :select]]]}))

(rf/reg-event-fx
 ::display-error-and-deactivate
 (fn [_ [_ _error]]
   {:fx [[:dispatch [::app.e/set-tool :select]]
         ;; REVIEW: Canceling also returns an error.
         #_[:dispatch [::notification.e/add [:div
                                             [:h2.pb-4.font-bold "EyeDropper cannot be activated."]
                                             [:div.text-error (str error)]]]]]}))
