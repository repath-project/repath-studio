(ns renderer.tools.dropper
  (:require [renderer.tools.base :as tools]
            [re-frame.core :as rf]
            [renderer.handlers :as handlers]
            [goog.color]))

(derive :dropper ::tools/misc)

(defmethod tools/properties :dropper
  []
  {:icon "eye-dropper"
   :description "Pick a color from your document."})

(defmethod tools/activate :dropper
  [db]
  ;; TODO side effect within db handler
  (if (.-EyeDropper js/window)
    (do (-> (js/EyeDropper.)
            (.open)
            (.then (fn [result]
                     (rf/dispatch [:elements/fill (.-sRGBHex result)])
                     (rf/dispatch [:document/set-fill (.-sRGBHex result)])
                     (rf/dispatch [:set-tool :select])))
            (.catch #(rf/dispatch [:set-tool :select])))
        (handlers/set-message db [:div "Click anywhere to pick a color."]))
    (tools/set-tool db :select)))



