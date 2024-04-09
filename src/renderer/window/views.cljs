(ns renderer.window.views
  (:require
   [platform]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.menubar.views :as menubar]))

(defn button
  [{:keys [icon action class]}]
  [:button.button.text-muted
   {:class ["px-3" class]
    :on-click #(rf/dispatch action)}
   [comp/icon icon]])

(defn window-control-buttons
  [maximized?]
  [{:action [:window/minimize]
    :icon "window-minimize"}
   {:action [:window/toggle-maximized]
    :icon (if maximized? "window-restore" "window-maximize")}
   {:action [:window/close]
    :icon "times"}])

(defn app-icon
  []
  [:div.drag
   [:img.ml-2.h-4.w-4
    {:src "img/icon-no-bg.svg"}]])

(defn app-header
  []
  (let [fullscreen? @(rf/subscribe [:window/fullscreen?])
        maximized? @(rf/subscribe [:window/maximized?])]
    [:div.flex.items-center.relative
     (when-not (or fullscreen? platform/mac?)
       [app-icon])
     [:div.flex.relative.bg-secondary
      {:class (when (and platform/mac? (not fullscreen?)) "ml-16")}
      [menubar/root]]
     [:div.absolute.flex.justify-center.drag.grow.h-full.items-center.pointer-events-none
      {:class "left-1/2 -translate-x-1/2"
       :style {:z-index -1}}
      @(rf/subscribe [:document/title-bar])]
     [:div.flex.h-full.flex-1.drag]
     [button {:action [:theme/cycle-mode]
              :icon (name @(rf/subscribe [:theme/mode]))
              :class "bg-primary"}]
     (when (and platform/electron? (not fullscreen?) (not platform/mac?))
       (into [:div.text-right]
             (map button (window-control-buttons maximized?))))
     (when fullscreen?
       [button {:action [:window/toggle-fullscreen]
                :icon "arrow-minimize"}])]))
