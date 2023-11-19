(ns renderer.window.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [platform :as platform]
   [renderer.status]
   [renderer.menubar.views :as menubar]))

(defn window-control-button
  [{:keys [icon action]}]
  [:button.button.text-muted.window-control-button
   {:on-click #(rf/dispatch action)}
   [comp/icon icon]])

(defn window-controls
  []
  (into [:div.text-right.drag]
        (mapv window-control-button
              [{:action [:window/minimize]
                :icon "window-minimize"}
               {:action [:window/toggle-maximized]
                :icon (if @(rf/subscribe [:window/maximized?])
                        "window-restore"
                        "window-maximize")}
               {:action [:window/close]
                :icon "times"}])))

(defn title-bar []
  (let [title @(rf/subscribe [:document/title])]
    [:div.title-bar title]))

(defn app-header []
  (when-not @(rf/subscribe [:window/fullscreen?])
    [:div.flex.items-center
     [:div.drag
      [:img.ml-2.mr-1
       {:src "img/icon-no-bg.svg"
        :style {:width "14px"
                :height "14px"}}]]
     [menubar/root]
     [title-bar]
     (let [theme-mode @(rf/subscribe [:theme/mode])]
       [:div.level-2
        {:class (when-not platform/electron? "mr-1.5")}
        [comp/icon-button
         (name theme-mode)
         {:on-click #(rf/dispatch [:theme/cycle-mode])}]])
     (when platform/electron? [window-controls])]))