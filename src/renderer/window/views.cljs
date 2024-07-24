(ns renderer.window.views
  (:require
   [platform]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.document.subs :as-alias document.s]
   [renderer.menubar.views :as menubar]
   [renderer.window.events :as-alias window.e]
   [renderer.window.subs :as-alias window.s]))

(defn button
  [{:keys [icon action class title]}]
  [:button.button.text-muted.focus:outline-none
   {:class ["px-3" class]
    :title title
    :on-click #(rf/dispatch action)}
   [comp/icon icon]])

(defn window-control-buttons
  [maximized?]
  [{:action [::window.e/minimize]
    :title "Minimize"
    :icon "window-minimize"}
   {:action [::window.e/toggle-maximized]
    :title (if maximized? "Restore" "Maximize")
    :icon (if maximized? "window-restore" "window-maximize")}
   {:action [::window.e/close]
    :title "Close"
    :icon "times"}])

(defn app-icon
  []
  [:div.drag
   [:img.ml-2.h-4.w-4
    {:src "img/icon-no-bg.svg"
     :alt "logo"}]])

(defn app-header
  []
  (let [fullscreen? @(rf/subscribe [::window.s/fullscreen?])
        maximized? @(rf/subscribe [::window.s/maximized?])]
    [:div.flex.items-center.relative
     (when-not (or fullscreen? platform/mac?)
       [app-icon])
     [:div.flex.relative.bg-secondary
      {:class (when (and platform/mac? (not fullscreen?)) "ml-16")}
      [menubar/root]]
     [:div.absolute.flex.justify-center.drag.grow.h-full.items-center.pointer-events-none
      {:class "left-1/2 -translate-x-1/2"
       :style {:z-index -1}}
      @(rf/subscribe [::document.s/title-bar])]
     [:div.flex.h-full.flex-1.drag]
     [button {:action [:theme/cycle-mode]
              :title "Theme mode"
              :icon (name @(rf/subscribe [:theme/mode]))
              :class "bg-primary"}]
     (when (and platform/electron? (not fullscreen?) (not platform/mac?))
       (into [:div.text-right]
             (map button (window-control-buttons maximized?))))
     (when fullscreen?
       [button {:action [::window.e/toggle-fullscreen]
                :icon "arrow-minimize"}])]))
