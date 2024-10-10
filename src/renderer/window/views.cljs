(ns renderer.window.views
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.menubar.views :as menubar]
   [renderer.theme.events :as-alias theme.e]
   [renderer.theme.subs :as-alias theme.s]
   [renderer.ui :as ui]
   [renderer.utils.system :as system]
   [renderer.window.events :as-alias window.e]
   [renderer.window.subs :as-alias window.s]))

(defn button
  [{:keys [icon action class title]}]
  [:button.button.text-muted.focus:outline-none
   {:class ["px-3" class]
    :title title
    :on-click #(rf/dispatch action)}
   [ui/icon icon]])

(defn window-control-buttons
  [maximized]
  [{:action [::window.e/minimize]
    :title "Minimize"
    :icon "window-minimize"}
   {:action [::window.e/toggle-maximized]
    :title (if maximized "Restore" "Maximize")
    :icon (if maximized "window-restore" "window-maximize")}
   {:action [::window.e/close]
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
  (let [fullscreen @(rf/subscribe [::window.s/fullscreen])
        maximized @(rf/subscribe [::window.s/maximized])
        theme-mode (name @(rf/subscribe [::theme.s/mode]))]
    [:div.flex.items-center.relative
     (when-not (or fullscreen system/mac?)
       [app-icon])
     [:div.flex.relative.bg-secondary
      {:class (when (and system/mac? (not fullscreen)) "ml-16")}
      [menubar/root]]
     [:div.absolute.hidden.md:flex.justify-center.drag.grow.h-full.items-center.pointer-events-none
      {:class "left-1/2 -translate-x-1/2"
       :style {:z-index -1}}
      @(rf/subscribe [::document.s/title-bar])]
     [:div.flex.h-full.flex-1.drag]
     [button {:action [::theme.e/cycle-mode]
              :title (str "Theme mode - " theme-mode)
              :icon theme-mode
              :class "bg-primary"}]
     (when (and system/electron? (not fullscreen) (not system/mac?))
       (into [:div.text-right]
             (map button (window-control-buttons maximized))))
     (when fullscreen
       [button {:action [::window.e/toggle-fullscreen]
                :icon "arrow-minimize"}])]))
