(ns renderer.window.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [platform :as platform]
   ["@radix-ui/react-menubar" :as Menubar]))

(defn menu
  []
  [{:key :file
    :label "File"
    :items [{:key :new-file
             :text "New"
             :action [:document/new]}
            {:key :divider-1
             :type :separator}
            {:key :open-file
             :text "Open…"
             :action [:document/open]}
            {:key :divider-2
             :type :separator}
            {:key :save
             :text "Save"
             :action [:document/save]}
            {:key :save-as
             :text "Save as…"
             :action [:document/save-as]}
            {:key :save-all
             :text "Save all"
             :action [:document/save-all]}
            {:key :divider-3
             :type :separator}
            {:key :save-all
             :text "Close"
             :action [:document/close-active]}
            {:key :divider-4
             :type :separator}
            {:key :exit
             :text "Exit"
             :action [:window/close]}]}
   {:key :edit
    :label "Edit"
    :items [{:key :undo
             :text "Undo"
             :action [:history/undo 1]}
            {:key :redo
             :text "Redo"
             :action [:history/redo 1]}
            {:key :divider-1
             :type :separator}
            {:key :cut
             :text "Cut"
             :action [:elements/cut]}
            {:key :copy
             :text "Copy"
             :action [:elements/copy]}
            {:key :paste
             :text "Paste"
             :action [:elements/paste]}
            {:key :paste-in-place
             :text "Paste in place"
             :action [:elements/paste-in-place]}
            {:key :paste-styles
             :text "Paste styles"
             :action [:elements/paste-styles]}
            {:key :divider-2
             :type :separator}
            {:key :duplicate
             :text "Duplicate"
             :action [:elements/duplicate-in-place]}
            {:key :divider-3
             :type :separator}
            {:key :select-all
             :text "Select all"
             :action [:elements/select-all]}
            {:key :deselect-all
             :text "Deselect all"
             :action [:elements/deselect-all]}
            {:key :select-same-tags
             :text "Select same tags"
             :action [:elements/select-same-tags]}]}
   {:key :object
    :label "Object"
    :items [{:key :to-path
             :text "Object to path"
             :action [:elements/->path]}
            {:key :stroke-to-path
             :text "Stroke to path"
             :action [:elements/stroke->path]}
            {:key :divider-1
             :type :separator}
            {:key :group
             :text "Group"
             :action [:elements/group]}
            {:key :ungroup
             :text "Ungroup"
             :action [:elements/ungroup]}
            {:key :divider-2
             :type :separator}
            {:key :raise
             :text "Raise"
             :action [:elements/raise]}
            {:key :lower
             :text "Lower"
             :action [:elements/lower]}
            {:key :raise-to-top
             :text "Raise to top"
             :action [:elements/raise-to-top]}
            {:key :lower-to-bottom
             :text "Lower to bottom"
             :action [:elements/lower-to-bottom]}]}
   #_{:key :path
      :label "Path"
      :items [{:key :simplify
               :text "Simplify"
               :action [:elements/manipulate-path :simplify]}
              {:key :smooth
               :text "Smooth"
               :action [:elements/manipulate-path :smooth]}
              {:key :flatten
               :text "Flatten"
               :action [:elements/manipulate-path :flatten]}
              {:key :reverse
               :text "Reverse"
               :action [:elements/manipulate-path :reverse]}]}
   {:key :view
    :label "View"
    :items [{:key :toggle-fullscreen
             :text "Fullscreen"
             :type :checkbox
             :checked? [:window/fullscreen?]
             :action [:window/toggle-fullscreen]}
            {:key :divider-1
             :type :separator}
            {:key :toggle-tree
             :text "Tree side bar"
             :type :checkbox
             :checked? [:window/sidebar? :tree]
             :action [:window/toggle-sidebar :tree]}
            {:key :toggle-properties
             :text "Properties side bar"
             :type :checkbox
             :checked? [:window/sidebar? :properties]
             :action [:window/toggle-sidebar :properties]}
            {:key :toggle-header-menu
             :type :checkbox
             :text "Menu bar"
             :checked? [:window/header?]
             :action [:window/toggle-header]}
            {:key :divider-2
             :type :separator}
            {:key :toggle-command-history
             :type :checkbox
             :text "Command history"
             :checked? [:window/repl-history?]
             :action [:window/toggle-repl-history]}]}
   {:key :help
    :label "Help"
    :items [{:key :getting-started
             :text "Getting started"
             :action [:window/open-remote-url "https://repath.studio/docs/getting-started/"]}
            {:key :support-us
             :text "Support us"
             :action [:window/open-remote-url "https://repath.studio/contribute/support-us/"]}
            {:key :source-code
             :text "Source code"
             :action [:window/open-remote-url "https://github.com/re-path/studio"]}]}])

(defn menu-button
  [{:keys [label items]}]
  [:> Menubar/Menu
   [:> Menubar/Trigger {:class "menubar-trigger"} label]
   [:> Menubar/Portal
    (into [:> Menubar/Content {:class "menu-content" :align "start"}]
          (map (fn [{:keys [type text action icon checked?]}]
                 (case type
                   :separator
                   [:> Menubar/Separator {:class "menu-separator"}]

                   :checkbox
                   [:> Menubar/CheckboxItem
                    {:class "menu-checkbox-item inset"
                     :onSelect #(rf/dispatch action)
                     :checked @(rf/subscribe checked?)}
                    [:> Menubar/ItemIndicator
                     {:class "menu-item-indicator"}
                     [comp/icon "checkmark"]]
                    text
                    [:div {:class "right-slot"}
                     [comp/shortcuts action]]]

                   [:> Menubar/Item
                    {:class "menu-item"
                     :onSelect #(rf/dispatch action)}
                    (when icon
                      [comp/icon icon {:class "menu-item-indicator"}])
                    text
                    [:div {:class "right-slot"}
                     [comp/shortcuts action]]])) items))]])

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
     [:div
      [:> Menubar/Root
       {:class "menubar-root"
        :onValueChange #(rf/dispatch [:set-backdrop (seq %)])}
       (map (fn [item] ^{:key item} [menu-button item]) (menu))]]
     [title-bar]
     (let [theme-mode @(rf/subscribe [:window/theme-mode])]
       [:div.level-2
        {:class (when-not platform/electron? "mr-1.5")}
        [comp/icon-button {:icon (name theme-mode)
                           :action #(rf/dispatch [:window/cycle-theme-mode])}]])
     (when platform/electron? [window-controls])]))
