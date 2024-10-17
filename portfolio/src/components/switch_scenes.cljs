(ns components.switch-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.ui :as ui]))

(defscene switch
  :title "Default switch"
  :param (atom true)
  [store]
  [:div.toolbar.bg-primary.flex.gap-2
   [ui/switch "Switch label" {:id "default-switch"
                              :default-checked @store
                              :on-checked-change (fn [v] (reset! store v))}]
   (str "Checked: " @store)])

(defscene switch-disabled
  :title "Disabled switch"
  :param (atom true)
  [store]
  [:div.toolbar.bg-primary.flex.gap-2
   [ui/switch "Switch label" {:id "disabled-switch"
                              :disabled true
                              :default-checked @store
                              :on-checked-change (fn [v] (reset! store v))}]
   (str "Checked: " @store)])

(defscene switch-custom
  :title "Custom color switch"
  :param (atom true)
  [store]
  [:div.toolbar.bg-primary.flex.gap-2
   [ui/switch "Switch label" {:id "disabled-switch"
                              :class "data-[state=checked]:bg-white"
                              :default-checked @store
                              :on-checked-change (fn [v] (reset! store v))}]
   (str "Checked: " @store)])
