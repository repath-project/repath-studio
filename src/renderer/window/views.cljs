(ns renderer.window.views
  (:require
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.menubar.views :as menubar.views]
   [renderer.theme.events :as-alias theme.events]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.views :as views]
   [renderer.window.events :as-alias window.events]
   [renderer.window.subs :as-alias window.subs]))

(defn button
  [{:keys [icon action class title]}]
  [:button.button.text-muted.focus:outline-none
   {:class ["px-3" class]
    :title title
    :on-click #(rf/dispatch action)}
   [views/icon icon]])

(defn window-control-buttons
  [maximized]
  [{:action [::window.events/minimize]
    :title "Minimize"
    :icon "window-minimize"}
   {:action [::window.events/toggle-maximized]
    :title (if maximized "Restore" "Maximize")
    :icon (if maximized "window-restore" "window-maximize")}
   {:action [::window.events/close]
    :title "Close"
    :icon "window-close"}])

(defn app-icon
  []
  [:div.drag
   [:img.ml-2.h-4.w-4
    {:src "img/icon-no-bg.svg"
     :alt "logo"}]])

(defn app-header
  []
  (let [fullscreen? @(rf/subscribe [::window.subs/fullscreen?])
        maximized? @(rf/subscribe [::window.subs/maximized?])
        theme-mode (name @(rf/subscribe [::theme.subs/mode]))
        mac? @(rf/subscribe [::app.subs/mac?])
        electron? @(rf/subscribe [::app.subs/electron?])]
    [:div.flex.items-center.relative
     (when-not (or fullscreen? mac?)
       [app-icon])
     [:div.flex.relative.bg-secondary
      {:class (when (and mac? (not fullscreen?)) "ml-16")}
      [menubar.views/root]]
     [:div.absolute.hidden.justify-center.drag.grow.h-full.items-center
      {:class "pointer-events-none md:flex left-1/2 -translate-x-1/2"
       :style {:z-index -1}}
      @(rf/subscribe [::document.subs/title-bar])]
     [:div.flex.h-full.flex-1.drag]
     [button {:action [::theme.events/cycle-mode]
              :title (str "Theme mode - " theme-mode)
              :icon theme-mode
              :class "bg-primary"}]
     (when (and electron? (not fullscreen?) (not mac?))
       (into [:div.text-right]
             (map button (window-control-buttons maximized?))))
     (when fullscreen?
       [button {:action [::window.events/toggle-fullscreen]
                :icon "arrow-minimize"}])]))
