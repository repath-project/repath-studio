(ns renderer.window.views
  (:require
   [platform]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.menubar.views :as menubar]))

(defn window-control-button
  [{:keys [icon action]}]
  [:button.button.text-muted.window-control-button
   {:on-click #(rf/dispatch action)}
   [comp/icon icon]])

(defn window-controls
  []
  (into [:div.text-right]
        (map window-control-button
             [{:action [:window/minimize]
               :icon "window-minimize"}
              {:action [:window/toggle-maximized]
               :icon (if @(rf/subscribe [:window/maximized?])
                       "window-restore"
                       "window-maximize")}
              {:action [:window/close]
               :icon "times"}])))

(defn app-icon
  []
  [:div.drag
   [:img.ml-2.h-4.w-4
    {:src "img/icon-no-bg.svg"}]])

(defn app-header
  []
  (let [fullscreen? @(rf/subscribe [:window/fullscreen?])
        title (or @(rf/subscribe [:document/path])
                  @(rf/subscribe [:document/title]))]
    [:div.flex.items-center.relative
     (when-not (or fullscreen? platform/mac?)
       [app-icon])
     [:div.flex.relative.bg-secondary
      {:class (when (and platform/mac? (not fullscreen?)) "ml-16")}
      [menubar/root]]
     [:div.title-bar (when title (str title " - ")) "Repath Studio"]
     [:div.flex.h-full.flex-1.drag]
     [:div.bg-primary
      {:class (when-not (or (and platform/electron? (not platform/mac?))
                            fullscreen?) "mr-1.5")}
      [comp/icon-button
       (name @(rf/subscribe [:theme/mode]))
       {:on-click #(rf/dispatch [:theme/cycle-mode])
        :class "rounded-none"}]]
     (when (and platform/electron? (not fullscreen?) (not platform/mac?))
       [window-controls])
     (when fullscreen?
       [window-control-button
        {:action [:window/toggle-fullscreen]
         :icon "arrow-minimize"}])]))
