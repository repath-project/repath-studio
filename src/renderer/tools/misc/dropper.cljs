(ns renderer.tools.misc.dropper
  (:require
   [re-frame.core :as rf]
   [renderer.handlers :as h]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.tools.base :as tools]))

(derive :dropper ::tools/misc)

(defmethod tools/properties :dropper
  []
  {:icon "eye-dropper"
   :description "Pick a color from your document."})

(defmethod tools/activate :dropper
  [db]
  ;; REVIEW: side effect within db handler
  (if (.-EyeDropper js/window)
    (do (-> (js/EyeDropper.)
            (.open)
            (.then (fn [^js/Object result]
                     (rf/dispatch [:element/fill (.-sRGBHex result)])
                     (rf/dispatch [:document/set-fill (.-sRGBHex result)])
                     (rf/dispatch [:set-tool :select])))
            (.catch (fn [error]
                      (rf/dispatch [:notification/add {:content [:div
                                                                 [:h2.pb-4.text-md "EyeDropper cannot be activated."]
                                                                 [:div.text-error (str error)]]}])
                      (rf/dispatch [:set-tool :select]))))
        (h/set-message db [:div "Click anywhere to pick a color."]))
    (-> db
        (notification.h/add
         (notification.v/unavailable-feature
          "EyeDropper"
          "https://developer.mozilla.org/en-US/docs/Web/API/EyeDropper_API#browser_compatibility"))
        (tools/set-tool :select))))
