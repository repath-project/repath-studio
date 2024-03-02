(ns renderer.window.views
  (:require
   [platform :as platform]
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

(defn app-header
  []
  (let [fullscreen? @(rf/subscribe [:window/fullscreen?])
        title (or @(rf/subscribe [:document/path])
                  @(rf/subscribe [:document/title]))]
    [:div.flex.items-center.relative
     (when-not fullscreen?
       [:div.drag
        [:img.ml-2.mr-1
         {:src "img/icon-no-bg.svg"
          :style {:width "14px"
                  :height "14px"}}]])
     [:div.flex.relative.bg-secondary
      [menubar/root]]
     [:div.title-bar (when title (str title " - ")) "Repath Studio"]
     [:div.flex.h-full.flex-1.drag]
     [:div.bg-primary
      {:class (when-not (or platform/electron? fullscreen?) "mr-1.5")}
      [comp/icon-button
       (name @(rf/subscribe [:theme/mode]))
       {:on-click #(rf/dispatch [:theme/cycle-mode])}]]
     (when (and platform/electron? (not fullscreen?))
       [window-controls])
     (when fullscreen?
       [window-control-button
        {:action [:window/toggle-fullscreen]
         :icon "arrow-minimize"}])]))
