(ns components.button-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.ui :as ui]))

(defscene icon-buttons
  :title "Icon buttons"
  [:div.toolbar.bg-primary
   [ui/icon-button "download" {:title "download"
                               :on-click #(js/alert "Downloaded")}]
   [ui/icon-button "folder" {:title "open"
                             :on-click #(js/alert "Opened")}]
   [ui/icon-button "save" {:title "save"
                           :disabled true
                           :on-click #(js/alert "Saved")}]])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defscene radio-icon-buttons
  :title "Radio icon buttons"
  :params (atom false)
  [store]
  [:div.toolbar.bg-primary
   [ui/radio-icon-button "refresh" @store
    {:title "Replay"
     :on-click #(swap! store not)}]])
