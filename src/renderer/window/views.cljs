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
             :label "New"
             :icon "file"
             :action [:document/new]}
            {:key :divider-1
             :type :separator}
            {:key :open-file
             :label "Open…"
             :icon "folder"
             :action [:document/open]}
            {:key :divider-2
             :type :separator}
            {:key :save
             :label "Save"
             :icon "save"
             :action [:document/save]}
            {:key :save-as
             :label "Save as…"
             :action [:document/save-as]}
            {:key :save-all
             :label "Save all"
             :action [:document/save-all]}
            {:key :divider-3
             :type :separator}
            {:key :close
             :label "Close"
             :action [:document/close-active]}
            {:key :divider-4
             :type :separator}
            {:key :exit
             :label "Exit"
             :action [:window/close]}]}
   {:key :edit
    :label "Edit"
    :items [{:key :undo
             :label "Undo"
             :action [:history/undo]}
            {:key :redo
             :label "Redo"
             :action [:history/redo]}
            {:key :divider-1
             :type :separator}
            {:key :cut
             :label "Cut"
             :action [:elements/cut]}
            {:key :copy
             :label "Copy"
             :action [:elements/copy]}
            {:key :paste
             :label "Paste"
             :action [:elements/paste]}
            {:key :paste-in-place
             :label "Paste in place"
             :action [:elements/paste-in-place]}
            {:key :paste-styles
             :label "Paste styles"
             :action [:elements/paste-styles]}
            {:key :divider-2
             :type :separator}
            {:key :duplicate
             :label "Duplicate"
             :action [:elements/duplicate-in-place]}
            {:key :divider-3
             :type :separator}
            {:key :select-all
             :label "Select all"
             :action [:elements/select-all]}
            {:key :deselect-all
             :label "Deselect all"
             :action [:elements/deselect-all]}
            {:key :invert-selection
             :label "Invert selection"
             :action [:elements/invert-selection]}
            {:key :select-same-tags
             :label "Select same tags"
             :action [:elements/select-same-tags]}]}
   {:key :object
    :label "Object"
    :items [{:key :to-path
             :label "Object to path"
             :action [:elements/->path]}
            {:key :stroke-to-path
             :label "Stroke to path"
             :action [:elements/stroke->path]}
            {:key :divider-1
             :type :separator}
            {:key :group
             :label "Group"
             :action [:elements/group]}
            {:key :ungroup
             :label "Ungroup"
             :action [:elements/ungroup]}
            {:key :divider-2
             :type :separator}
            {:key :raise
             :label "Raise"
             :action [:elements/raise]}
            {:key :lower
             :label "Lower"
             :action [:elements/lower]}
            {:key :raise-to-top
             :label "Raise to top"
             :action [:elements/raise-to-top]}
            {:key :lower-to-bottom
             :label "Lower to bottom"
             :action [:elements/lower-to-bottom]}]}
   #_{:key :path
      :label "Path"
      :items [{:key :simplify
               :label "Simplify"
               :action [:elements/manipulate-path :simplify]}
              {:key :smooth
               :label "Smooth"
               :action [:elements/manipulate-path :smooth]}
              {:key :flatten
               :label "Flatten"
               :action [:elements/manipulate-path :flatten]}
              {:key :reverse
               :label "Reverse"
               :action [:elements/manipulate-path :reverse]}]}
   {:key :view
    :label "View"
    :items [{:key :toggle-fullscreen
             :label "Fullscreen"
             :type :checkbox
             :checked? [:window/fullscreen?]
             :action [:window/toggle-fullscreen]}
            {:key :divider-1
             :type :separator}
            {:key :toggle-tree
             :label "Tree side bar"
             :type :checkbox
             :checked? [:panel/visible? :tree]
             :action [:panel/toggle :tree]}
            {:key :toggle-properties
             :label "Properties side bar"
             :type :checkbox
             :checked? [:panel/visible? :properties]
             :action [:panel/toggle :properties]}
            {:key :toggle-xml
             :label "XML view"
             :type :checkbox
             :checked? [:panel/visible? :xml]
             :action [:panel/toggle :xml]}
            {:key :toggle-header-menu
             :type :checkbox
             :label "Menu bar"
             :checked? [:window/header?]
             :action [:window/toggle-header]}
            {:key :divider-2
             :type :separator}
            {:key :toggle-grid
             :type :checkbox
             :label "Grid"
             :checked? [:grid?]
             :action [:toggle-grid]}
            {:key :toggle-rulers
             :type :checkbox
             :label "Rulers"
             :checked? [:rulers?]
             :action [:toggle-rulers]}
            {:key :toggle-command-history
             :type :checkbox
             :label "Command history"
             :checked? [:panel/visible? :repl-history]
             :action [:panel/toggle :repl-history]}]}
   {:key :help
    :label "Help"
    :items [{:key :website
             :label "Website"
             :action [:window/open-remote-url "https://repath.studio/"]}
            {:key :source-code
             :label "Source Code"
             :action [:window/open-remote-url "https://github.com/re-path/studio"]}
            {:key :changelog
             :label "Changelog"
             :action [:window/open-remote-url "https://repath.studio/roadmap/changelog/"]}
            {:key :divider-1
             :type :separator}
            {:key :submit-issue
             :label "Submit an issue"
             :action [:window/open-remote-url "https://github.com/re-path/studio/issues/new/choose"]}]}])

(defn menu-button
  [{:keys [label items]}]
  [:> Menubar/Menu
   [:> Menubar/Trigger {:class "menubar-trigger"} label]
   [:> Menubar/Portal
    (into [:> Menubar/Content {:class "menu-content" :align "start" :loop true}]
          (map (fn [{:keys [type label action _icon checked?]}]
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
                    label
                    [:div {:class "right-slot"}
                     [comp/shortcuts action]]]

                   [:> Menubar/Item
                    {:class "menu-item"
                     :onSelect #(rf/dispatch action)}
                    #_(when icon
                        [comp/icon icon {:class "menu-item-indicator"}])
                    label
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
       (map (fn [item] ^{:key item} [menu-button item]) (menu))
       [:button.button.px-3.flex.items-center
        {:on-click #(rf/dispatch [:cmdk/toggle])}
        "Search…"]]
      ]
     [title-bar]
     (let [theme-mode @(rf/subscribe [:theme/mode])]
       [:div.level-2
        {:class (when-not platform/electron? "mr-1.5")}
        [comp/icon-button
         (name theme-mode)
         {:on-click #(rf/dispatch [:theme/cycle-mode])}]])
     (when platform/electron? [window-controls])]))