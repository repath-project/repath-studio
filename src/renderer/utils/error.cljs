(ns renderer.utils.error
  (:require
   [reagent.core :as ra]
   [re-frame.core :as rf]))

(defn boundary
  []
  (let [error-state (ra/atom nil)]
    (ra/create-class
     {:component-did-catch (fn [_this error info]
                             (reset! error-state [error info]))
      :get-derived-state-from-error #(rf/dispatch [:history/cancel])
      :reagent-render (fn [& children]
                        (when @error-state
                          [:div
                           "Something went wrong."
                           [:input
                            {:type "button"
                             :on-click #(reset! error-state nil)
                             :value "Try again"}]])
                        (into [:<>] children))})))