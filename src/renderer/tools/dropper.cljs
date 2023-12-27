(ns renderer.tools.dropper
  (:require
   [goog.color]
   [re-frame.core :as rf]
   [renderer.handlers :as handlers]
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
            (.then (fn [result]
                     (rf/dispatch [:element/fill (.-sRGBHex result)])
                     (rf/dispatch [:document/set-fill (.-sRGBHex result)])
                     (rf/dispatch [:set-tool :select])))
            (.catch (fn [error]
                      (rf/dispatch [:notification/add {:content [:div
                                                                 [:h2.pb-4.text-md "EyeDropper cannot be activated."]
                                                                 [:div.text-error (str error)]]}])
                      (rf/dispatch [:set-tool :select]))))
        (handlers/set-message db [:div "Click anywhere to pick a color."]))
    (-> db
        (update :notifications conj "Your browser does not support the EyeDropper API.")
        (tools/set-tool :select))))
