(ns renderer.window.views
  (:require
   [platform]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.menubar.views :as menubar]))

(defn button
  [{:keys [icon action]}]
  [:button.button.text-muted.window-control-button
   {:on-click #(rf/dispatch action)}
   [comp/icon icon]])

(def window-control-buttons
  [{:action [:window/minimize]
    :icon "window-minimize"}
   {:action [:window/toggle-maximized]
    :icon (if @(rf/subscribe [:window/maximized?])
            "window-restore"
            "window-maximize")}
   {:action [:window/close]
    :icon "times"}])

(defn app-icon
  []
  [:div.drag
   [:img.ml-2.h-4.w-4
    {:src "img/icon-no-bg.svg"}]])

(defn app-header
  []
  (let [fullscreen? @(rf/subscribe [:window/fullscreen?])]
    [:div.flex.items-center.relative
     (when-not (or fullscreen? platform/mac?)
       [app-icon])
     [:div.flex.relative.bg-secondary
      {:class (when (and platform/mac? (not fullscreen?)) "ml-16")}
      [menubar/root]]
     [:div.title-bar @(rf/subscribe [:document/title-bar])]
     [:div.flex.h-full.flex-1.drag]
     [:div.bg-primary
      [button {:action [:theme/cycle-mode]
               :icon (name @(rf/subscribe [:theme/mode]))}]]
     (when (and platform/electron? (not fullscreen?) (not platform/mac?))
       (into [:div.text-right]
             (map button window-control-buttons)))
     (when fullscreen?
       [button {:action [:window/toggle-fullscreen]
                :icon "arrow-minimize"}])]))
