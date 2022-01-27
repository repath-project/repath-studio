(ns repath.studio.error
    (:require [reagent.core :as ra]))

(defn boundary [& children]
  (let [error-state (ra/atom nil)]
    (ra/create-class
     {:constructor (fn [this props])
      :component-did-catch (fn [error info] (reset! error-state [error info]))
      :get-derived-state-from-error (fn [error info] (reset! error-state [error info]))
      :reagent-render (fn [& children]
                        (when @error-state
                          [:div
                           "Something went wrong."
                           [:input
                            {:type "button"
                             :on-click #(reset! error-state nil)
                             :value "Try again"}]])
                          (into [:<>] children))})))