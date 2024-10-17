(ns components.slider-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.ui :as ui]))

(defscene slider
  :title "Deafult slider"
  :params (atom [25])
  [store]
  [:div.toolbar.bg-primary.flex.gap-2
   [:div.w-64.h-8
    [ui/slider {:min 0
                :max 50
                :step 1
                :default-value @store
                :on-value-change (fn [v] (reset! store v))}]]
   [:div (first @store)]])

(defscene slider-disabled
  :title "Disabled slider"
  :params (atom [25])
  [store]
  [:div.toolbar.bg-primary.flex.gap-2
   [:div.w-64.h-8
    [ui/slider {:min 0
                :max 50
                :step 1
                :disabled true
                :default-value @store
                :on-value-change (fn [v] (reset! store v))}]]
   [:div (first @store)]])
