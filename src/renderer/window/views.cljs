(ns renderer.window.views
  (:require
   [i18n :refer [t]]
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
        (mapv window-control-button
              [{:action [:window/minimize]
                :icon "window-minimize"}
               {:action [:window/toggle-maximized]
                :icon (if @(rf/subscribe [:window/maximized?])
                        "window-restore"
                        "window-maximize")}
               {:action [:window/close]
                :icon "times"}])))

(defn app-header []
  (when-not @(rf/subscribe [:window/fullscreen?])
    [:div.flex.items-center.relative
     [:div.drag
      [:img.ml-2.mr-1
       {:src "img/icon-no-bg.svg"
        :style {:width "14px"
                :height "14px"}}]]
     [:div.flex.relative.level-0
      [menubar/root]
      [:button.button.px-3.flex.items-center
       {:on-click #(rf/dispatch [:cmdk/toggle])}
       (t [:cmdk/search "Search…"])]]
     [:div.title-bar @(rf/subscribe [:document/title])]
     [:div.flex.h-full.flex-1.drag]
     [:div.level-2
      {:class (when-not platform/electron? "mr-1.5")}
      [comp/icon-button
       (name @(rf/subscribe [:theme/mode]))
       {:on-click #(rf/dispatch [:theme/cycle-mode])}]]
     (when platform/electron? [window-controls])]))
