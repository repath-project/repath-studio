(ns renderer.utils.error
  (:require
   [re-frame.core :as rf]
   [reagent.core :as ra]))

(def error-message
  "Your last action was canceled due to an error.")

(defn boundary
  []
  (ra/create-class
   {:component-did-catch
    (fn [_this _error _info]
      (rf/dispatch [:notification/add {:content error-message}]))

    :get-derived-state-from-error
    #(rf/dispatch [:history/cancel])

    :reagent-render
    (fn [& children]
      (into [:<>] children))}))
