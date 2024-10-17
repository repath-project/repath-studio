(ns components.switch-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.ui :as ui]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defscene switch
  :title "Default switch"
  :params (atom true)
  [store]
  [:div.toolbar.bg-primary.flex.gap-2.h-10
   [ui/switch "Switch label" {:id "default-switch"
                              :default-checked @store
                              :on-checked-change (fn [v] (reset! store v))}]
   (str "Checked: " @store)])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defscene switch-disabled
  :title "Disabled switch"
  :params (atom true)
  [store]
  [:div.toolbar.bg-primary.flex.gap-2.h-10
   [ui/switch "Switch label" {:id "disabled-switch"
                              :disabled true
                              :default-checked @store
                              :on-checked-change (fn [v] (reset! store v))}]
   (str "Checked: " @store)])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defscene switch-custom
  :title "Custom color switch"
  :params (atom true)
  [store]
  [:div.toolbar.bg-primary.flex.gap-2.h-10
   [ui/switch "Switch label" {:id "disabled-switch"
                              :class "data-[state=checked]:bg-white"
                              :default-checked @store
                              :on-checked-change (fn [v] (reset! store v))}]
   (str "Checked: " @store)])
