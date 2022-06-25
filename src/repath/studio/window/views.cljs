(ns repath.studio.window.views
  (:require
   [re-frame.core :as rf]
   [repath.studio.components :as comp]
   [repath.studio.styles :as styles]
   ["@fluentui/react" :as fui]))

(def menu [{:key :file
            :label "File"
            :items [{:key :new-file
                     :text "New File"
                     :secondaryText "Ctrl+N"
                     :onClick #(rf/dispatch [:document/new])}
                    {:key :divider-1
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :save
                     :text "Save"
                     :secondaryText "Ctrl+S"
                     :onClick #(rf/dispatch [:document/save])}
                    {:key :save-as
                     :text "Save As"
                     :secondaryText "Ctrl+Shift+S"}
                    {:key :save-all
                     :text "Save All"
                     :onClick #(rf/dispatch [:document/save-all])}
                    {:key :divider-2
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :divider-3
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :exit
                     :text "Exit"
                     :onClick #(rf/dispatch [:window/close])}]}
           {:key :edit
            :label "Edit"
            :items [{:key :undo
                     :text "Undo"
                     :secondaryText "Ctrl+Z"
                     :onClick #(rf/dispatch [:history/undo 1])}
                    {:key :redp
                     :text "Redo"
                     :secondaryText "Ctrl+Shift+Z"
                     :onClick #(rf/dispatch [:history/redo 1])}
                    {:key :divider-1
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :cut
                     :text "Cut"
                     :secondaryText "Ctrl+X"
                     :onClick #(rf/dispatch [:elements/cut])}
                    {:key :copy
                     :text "Copy"
                     :secondaryText "Ctrl+C"
                     :onClick #(rf/dispatch [:elements/copy])}
                    {:key :paste
                     :text "Paste"
                     :secondaryText "Ctrl+V"
                     :onClick #(rf/dispatch [:elements/paste])}
                    {:key :paste-in-place
                     :text "Paste in place"
                     :secondaryText "Ctrl+Alt+V"
                     :onClick #(rf/dispatch [:elements/paste-in-position])}
                    {:key :paste-styles
                     :text "Paste styles"
                     :secondaryText "Ctrl+Shift+V"
                     :onClick #(rf/dispatch [:elements/paste-styles])}
                    {:key :divider-2
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :duplicate
                     :text "Duplicate"
                     :secondaryText "Ctrl+D"
                     :onClick #(rf/dispatch [:elements/duplicate-in-posistion])}
                    {:key :divider-1
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :select-all
                     :text "Select all"
                     :secondaryText "Ctrl+A"
                     :onClick #(rf/dispatch [:elements/select-all])}
                    {:key :deselect-all
                     :text "Deselect all"
                     :secondaryText "Esc"
                     :onClick #(rf/dispatch [:elements/deselect-all])}
                    {:key :select-same-tag
                     :text "Select same tags"
                     :secondaryText "Ctrl+Shift+A"
                     :onClick #(rf/dispatch [:elements/deselect-all])}]}
           {:key :object
            :label "Object"
            :items [{:key :to-path
                     :text "Convert to path"
                     :secondaryText "Ctrl+Shift+P"
                     :onClick #(rf/dispatch [:elements/to-path])}
                    {:key :divider-1
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :group
                     :text "Group"
                     :secondaryText "Ctrl+G"
                     :onClick #(rf/dispatch [:elements/group])}
                    {:key :ungroup
                     :text "Ungroup"
                     :secondaryText "Ctrl+Shift+G"
                     :onClick #(rf/dispatch [:elements/ungroup])}
                    {:key :divider-2
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :raise
                     :text "Raise"
                     :secondaryText "Page Up"
                     :onClick #(rf/dispatch [:elements/raise])}
                    {:key :lower
                     :text "Lower"
                     :secondaryText "Page Down"
                     :onClick #(rf/dispatch [:elements/lower])}
                    {:key :raise-to-top
                     :text "Raise to top"
                     :secondaryText "Home"
                     :onClick #(rf/dispatch [:elements/raise-to-top])}
                    {:key :lower-to-bottom
                     :text "Lower to bottom"
                     :secondaryText "End"
                     :onClick #(rf/dispatch [:elements/lower-to-bottom])}]}
           {:key :view
            :label "View"
            :items [{:key :toggle-tree
                     :text "Toggle element tree"
                     :secondaryText "Ctrl+T"
                     :onClick #(rf/dispatch [:window/toggle-sidebar :tree])}
                    {:key :toggle-properties
                     :text "Toggle element properties"
                     :secondaryText "Ctrl+P"
                     :onClick #(rf/dispatch [:window/toggle-sidebar :properties])}
                    {:key :toggle-header-menu
                     :text "Toggle header menu"
                     :secondaryText "Ctrl+L"
                     :onClick #(rf/dispatch [:window/toggle-header])}]}
           {:key :help
            :label "Help"
            :items [{:key :getting-started
                     :text "Getting started"
                     :onClick #(rf/dispatch [:window/open-remote-url "https://repath.studio/docs/getting-started/"])}
                    {:key :support-us
                     :text "Support Us"
                     :onClick #(rf/dispatch [:window/open-remote-url "https://repath.studio/contribute/support-us/"])}
                    {:key :source-code
                     :text "Source Code"
                     :onClick #(rf/dispatch [:window/open-remote-url "https://github.com/re-path/studio"])}]}])

(defn menu-button [{:keys [key label items]}]
  [:> fui/DefaultButton {:key key
                         :text label
                         :class "muted"
                         :styles {:root {:border 0
                                         :min-width "auto"
                                         :padding "0 10px"}
                                  :menuIcon {:display "none"}}
                         :menuProps {:shouldFocusOnMount true
                                     :shouldFocusOnContainer true
                                     :items items}}])

(defn window-control-button
  [{:keys [icon action]}]
  [:button {:class "button muted window-control-button"
            :on-click #(rf/dispatch action)} [comp/icon {:icon icon}]])

(defn window-controls
  []
  (into [:div {:style {:flex "1 1 100%" :-webkit-app-region "drag" :text-align "right"}}]
        (mapv window-control-button [{:action [:window/minimize] :icon "window-minimize"}
                                     {:action [:window/toggle-maximized] :icon (if @(rf/subscribe [:window/maximized?]) "window-restore" "window-maximize")}
                                     {:action [:window/close] :icon "times"}])))

(defn title-bar []
  (let [title @(rf/subscribe [:title])]
    [:div.h-box {:class "title-bar"}  title]))

(defn app-header []
  (when-not @(rf/subscribe [:window/fullscreen?]) [:div.h-box
                                                   [:div.h-box {:style {:flex "1 1 100%" :-webkit-app-region "drag"}}
                                                    [:img {:src "img/icon-no-bg.svg"
                                                           :style {:padding styles/padding
                                                                   :width styles/icon-size
                                                                   :height styles/icon-size}}] (map menu-button menu)] [title-bar] [window-controls]]))
