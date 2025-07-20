(ns renderer.window.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.menubar.views :as menubar.views]
   [renderer.theme.events :as-alias theme.events]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]
   [renderer.window.events :as-alias window.events]
   [renderer.window.subs :as-alias window.subs]))

(defn language-dropdown
  []
  (let [lang @(rf/subscribe [::app.subs/lang])]
    [:> DropdownMenu/Root
     [:> DropdownMenu/Trigger
      {:as-child true}
      [:button.button
       {:class "flex gap-1 items-center px-2 uppercase text-muted bg-primary font-mono"}
       (take 2 lang)
       [views/icon "chevron-down"]]]
     [:> DropdownMenu/Portal
      [:> DropdownMenu/Content
       {:side "top"
        :align "end"
        :sideOffset 5
        :position "popper"
        :class "menu-content rounded-sm select-content"
        :on-key-down #(.stopPropagation %)
        :on-escape-key-down #(.stopPropagation %)}
       (for [{:keys [id label action checked]} (menubar.views/languages-submenu)]
         ^{:key id}
         [:> DropdownMenu/CheckboxItem
          {:class "menu-checkbox-item inset"
           :on-select #(rf/dispatch action)
           :checked checked}
          [:> DropdownMenu/ItemIndicator
           {:class "menu-item-indicator"}
           [views/icon "checkmark"]]
          [:div label]
          [:span.text-muted.uppercase.font-mono (take 2 id)]])]]]))

(defn button
  [{:keys [icon action class title]}]
  [:button.button.text-muted.focus:outline-none
   {:class ["px-3" class]
    :title title
    :on-click #(rf/dispatch action)}
   [views/icon icon]])

(defn window-control-buttons
  []
  (let [maximized @(rf/subscribe [::window.subs/maximized?])]
    [{:action [::window.events/minimize]
      :title (t [::minimize "Minimize"])
      :icon "window-minimize"}
     {:action [::window.events/toggle-maximized]
      :title (if maximized
               (t [::restore "Restore"])
               (t [::maximize "Maximize"]))
      :icon (if maximized
              "window-restore"
              "window-maximize")}
     {:action [::window.events/close]
      :title (t [::close "Close"])
      :icon "window-close"}]))

(defn app-icon
  []
  [:div.drag.shrink-0
   [:img.mx-2.h-4.w-4
    {:src "img/icon-no-bg.svg"
     :alt "logo"}]])

(defn app-header
  []
  (let [fullscreen? @(rf/subscribe [::window.subs/fullscreen?])
        theme-mode (name @(rf/subscribe [::theme.subs/mode]))
        mac? @(rf/subscribe [::app.subs/mac?])
        electron? @(rf/subscribe [::app.subs/electron?])
        title-bar @(rf/subscribe [::document.subs/title-bar])]
    [:div.flex.items-center.relative.gap-0.5
     (when-not (or fullscreen? mac?)
       [app-icon])
     [:div.overflow-hidden
      [views/scroll-area
       [:div.flex.relative.bg-secondary
        {:class (when (and mac? (not fullscreen?))
                  "ml-16")}
        [menubar.views/root]]]]
     [:div.absolute.hidden.justify-center.drag.grow.h-full.items-center
      {:class "pointer-events-none md:flex left-1/2 -translate-x-1/2"
       :style {:z-index -1}
       :dir "ltr"}
      title-bar]
     [:div.flex.h-full.flex-1.drag]
     [:div.flex
      [:div.flex.gap-px
       [language-dropdown]
       [button {:action [::theme.events/cycle-mode]
                :title (t [::theme "Theme mode - %1"] [theme-mode])
                :icon theme-mode
                :class "bg-primary"}]
       [button {:action [::window.events/toggle-fullscreen]
                :title (if fullscreen?
                         (t [::exit-fullscreen "Exit fullscreen"])
                         (t [::enter-fullscreen "Enter fullscreen"]))
                :icon (if fullscreen? "arrow-minimize" "arrow-maximize")
                :class "bg-primary hidden sm:block"}]
       (when (and electron?
                  (not fullscreen?)
                  (not mac?))
         (->> (window-control-buttons)
              (map button)
              (into [:div.flex])))]]]))
