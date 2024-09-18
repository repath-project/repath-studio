(ns components.button-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.ui :as ui]))

(defscene icon-buttons
  :title "Icon buttons"
  [:div.toolbar.bg-primary
   [ui/icon-button "download" {:title "download"
                               :on-click #(js/alert "Downloaded")}]
   [ui/icon-button "save" {:title "save"
                           :on-click #(js/alert "Saved")}]])

(defscene toggle-icon-buttons
  :params (atom {:visible true
                 :locked true})
  :title "Toggle icon buttons"
  [store]
  [:div.toolbar.bg-primary
   [ui/toggle-icon-button
    {:active? (:visible @store)
     :active-icon "eye"
     :active-text "hide"
     :inactive-icon "eye-closed"
     :inactive-text "show"
     :action #(swap! store update :visible not)}]
   [ui/toggle-icon-button
    {:active? (:locked @store)
     :active-icon "lock"
     :active-text "unlock"
     :inactive-icon "unlock"
     :inactive-text "lock"
     :action #(swap! store update :locked not)}]])

(defscene radio-icon-buttons
  :params (atom false)
  :title "Radio icon buttons"
  [store]
  [:div.toolbar.bg-primary
   [ui/radio-icon-button "refresh" @store
    {:title "Replay"
     :on-click #(swap! store not)}]])
