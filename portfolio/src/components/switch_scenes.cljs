(ns components.switch-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.ui :as ui]))

(defscene switch
  :title "Switch"
  [:div.toolbar.bg-primary
   [ui/switch {:id "default-switch"
               :label "Switch label"
               :default-checked? true}]])
