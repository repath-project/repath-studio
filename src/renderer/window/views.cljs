(ns renderer.window.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.theme.events :as-alias theme.events]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.utils.i18n :as utils.i18n :refer [t]]
   [renderer.views :as views]
   [renderer.window.events :as-alias window.events]
   [renderer.window.menubar :as window.menubar]
   [renderer.window.subs :as-alias window.subs]))

(defn language-item
  [{:keys [id label action checked abbr]} system-abbr]
  [:> DropdownMenu/CheckboxItem
   {:class "menu-checkbox-item inset"
    :on-select #(rf/dispatch action)
    :checked checked}
   [:> DropdownMenu/ItemIndicator
    {:class "menu-item-indicator"}
    [views/icon "checkmark"]]
   [:div label]
   (if (= id "system")
     [:span.uppercase.font-mono.text-foreground-disabled (or system-abbr "EN")]
     [:span.uppercase.font-mono.text-foreground-muted abbr])])

(defn language-dropdown
  []
  (let [computed-lang @(rf/subscribe [::app.subs/computed-lang])
        system-lang @(rf/subscribe [::app.subs/system-lang])
        system-abbr (get-in utils.i18n/languages [system-lang :abbr])
        computed-abbr (get-in utils.i18n/languages [computed-lang :abbr])]
    [:> DropdownMenu/Root
     [:> DropdownMenu/Trigger
      {:as-child true}
      [:button.button
       {:class "flex gap-1 items-center px-2 uppercase bg-primary font-mono
                outline-inset"}
       computed-abbr
       [views/icon "chevron-down"]]]
     [:> DropdownMenu/Portal
      [:> DropdownMenu/Content
       {:side "bottom"
        :align "end"
        :position "popper"
        :class "menu-content rounded-sm select-content"
        :on-key-down #(.stopPropagation %)
        :on-escape-key-down #(.stopPropagation %)}
       (for [lang (window.menubar/languages-submenu)]
         ^{:key (:id lang)}
         [language-item lang system-abbr])
       [:> DropdownMenu/Arrow {:class "fill-primary"}]]]]))

(defn button
  [{:keys [icon action class title]}]
  [:button.button.px-3.outline-inset
   {:class class
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

(defn app-icon []
  [:div.drag.shrink-0.px-1
   [:img.h-4.w-4
    {:src "img/icon-no-bg.svg"
     :alt "logo"}]])

(defn app-header
  []
  (let [fullscreen? @(rf/subscribe [::window.subs/fullscreen?])
        theme-mode (name @(rf/subscribe [::theme.subs/mode]))
        mac? @(rf/subscribe [::app.subs/mac?])
        web? @(rf/subscribe [::app.subs/web?])
        installable? @(rf/subscribe [::app.subs/installable?])
        title-bar @(rf/subscribe [::document.subs/title-bar])
        md? @(rf/subscribe [::window.subs/md?])]
    [:div.flex.relative
     [:div.md:p-1.gap-1.flex.items-center
      (when-not (or fullscreen? mac?)
        [app-icon])
      (when md?
        [:div.flex.relative.bg-secondary
         {:class (when (and mac? (not fullscreen?)) "ml-16")}
         [window.menubar/root]])]
     [:div.absolute.hidden.justify-center.drag.grow.h-full.items-center
      {:class "pointer-events-none md:flex left-1/2 -translate-x-1/2 z-[-1]"
       :dir "ltr"}
      title-bar]
     [:div.flex.h-full.flex-1.drag]
     [:div.flex
      [:div.flex.gap-px
       (when installable?
         [views/icon-button "download"
          {:title (t [::install])
           :class "rounded-none outline-inset bg-transparent!"
           :on-click #(rf/dispatch [::app.events/install])}])
       (when md?
         [:<>
          [language-dropdown]
          [button {:action [::theme.events/cycle-mode]
                   :title (t [::theme "Theme mode - %1"] [theme-mode])
                   :icon theme-mode
                   :class "bg-primary"}]])
       (when (or fullscreen? web?)
         [button {:action [::window.events/toggle-fullscreen]
                  :title (if fullscreen?
                           (t [::exit-fullscreen "Exit fullscreen"])
                           (t [::enter-fullscreen "Enter fullscreen"]))
                  :icon (if fullscreen? "arrow-minimize" "arrow-maximize")
                  :class "bg-primary"}])
       (when-not (or web? fullscreen? mac?)
         (->> (window-control-buttons)
              (map button)
              (into [:div.flex])))]]]))
