(ns renderer.tool.misc.dropper
  (:require
   [re-frame.core :as rf]
   [renderer.document.events :as-alias document.e]
   [renderer.element.events :as-alias element.e]
   [renderer.handlers :as h]
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
  ;; REVIEW: side effect within db handler
  (if (.-EyeDropper js/window)
    (do (-> (js/EyeDropper.)
            (.open)
            (.then (fn [^js/Object result]
                     (rf/dispatch [::element.e/fill (.-sRGBHex result)])
                     (rf/dispatch [::document.e/set-fill (.-sRGBHex result)])
                     (rf/dispatch [:set-tool :select])))
            (.catch (fn [error]
                      (rf/dispatch [::notification.e/add
                                    [:div
                                     [:h2.pb-4.font-bold "EyeDropper cannot be activated."]
                                     [:div.text-error (str error)]]])
                      (rf/dispatch [:set-tool :select]))))
        (h/set-message db "Click anywhere to pick a color."))
    (-> db
        (notification.h/add
         (notification.v/unavailable-feature
          "EyeDropper"
          "https://developer.mozilla.org/en-US/docs/Web/API/EyeDropper_API#browser_compatibility"))
        (h/set-tool :select))))
