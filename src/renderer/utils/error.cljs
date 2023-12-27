(ns renderer.utils.error
  "https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary"
  (:require
   [re-frame.core :as rf]
   [reagent.core :as ra]))

(def error-message
  "Your last action was canceled due to the following error:")

(defn boundary
  []
  (ra/create-class
   {;;https://react.dev/reference/react/Component#componentdidcatch
    :component-did-catch
    (fn [_this error _info]
      (rf/dispatch [:notification/add {:content [:div
                                                 [:h2.pb-4.text-md error-message]
                                                 [:div.text-error (str error)]]}]))
    
    ;; Try to revert to a working state
    ;; https://react.dev/reference/react/Component#static-getderivedstatefromerror
    :get-derived-state-from-error
    #(rf/dispatch-sync [:history/undo])

    :reagent-render
    (fn [& children]
      (into [:<>] children))}))
