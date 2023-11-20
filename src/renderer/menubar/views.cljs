(ns renderer.menubar.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   ["@radix-ui/react-menubar" :as Menubar]))

(def menu
  [{:key :file
    :label "File"
    :type :root
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
            {:key :exit
             :label "Exit"
             :action [:window/close]}]}
   {:key :edit
    :label "Edit"
    :type :root
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
             :action [:element/cut]}
            {:key :copy
             :label "Copy"
             :action [:element/copy]}
            {:key :paste
             :label "Paste"
             :action [:element/paste]}
            {:key :paste-in-place
             :label "Paste in place"
             :action [:element/paste-in-place]}
            {:key :paste-styles
             :label "Paste styles"
             :action [:element/paste-styles]}
            {:key :divider-2
             :type :separator}
            {:key :duplicate
             :label "Duplicate"
             :action [:element/duplicate-in-place]}
            {:key :divider-3
             :type :separator}
            {:key :select-all
             :label "Select all"
             :action [:element/select-all]}
            {:key :deselect-all
             :label "Deselect all"
             :action [:element/deselect-all]}
            {:key :invert-selection
             :label "Invert selection"
             :action [:element/invert-selection]}
            {:key :select-same-tags
             :label "Select same tags"
             :action [:element/select-same-tags]}
            {:key :divider-3
             :type :separator}
            {:key :delete
             :label "Delete"
             :action [:element/delete]}]}
   {:key :object
    :label "Object"
    :type :root
    :items [{:key :to-path
             :label "Object to path"
             :action [:element/->path]}
            {:key :stroke-to-path
             :label "Stroke to path"
             :action [:element/stroke->path]}
            {:key :divider-1
             :type :separator}
            {:key :group
             :label "Group"
             :action [:element/group]}
            {:key :ungroup
             :label "Ungroup"
             :action [:element/ungroup]}
            {:key :divider-2
             :type :separator}
            {:key :lock
             :label "Lock"
             :action [:element/lock]}
            {:key :unlock
             :label "Unlock"
             :action [:element/unlock]}
            {:key :divider-3
             :type :separator}
            {:key :path
             :label "Align"
             :type :sub-menu
             :items [{:key :align-left
                      :label "Left"
                      :action [:element/align :left]}
                     {:key :align-center-horizontally
                      :label "Center horizontally"
                      :action [:element/align :center-horizontal]}
                     {:key :align-right
                      :label "Right"
                      :action [:element/align :right]}
                     {:key :divider-1
                      :type :separator}
                     {:key :align-top
                      :label "Top"
                      :action [:element/align :top]}
                     {:key :align-center-vertically
                      :label "Center vertically"
                      :action [:element/align :center-vertical]}
                     {:key :align-bottom
                      :label "Bottom"
                      :action [:element/align :bottom]}]}
            {:key :divider-4
             :type :separator}
            {:key :boolean
             :label "Boolean operation"
             :type :sub-menu
             :items [{:key :exclude
                      :label "Exclude"
                      :action [:element/bool-operation :exclude]}
                     {:key :unite
                      :label "Unite"
                      :action [:element/bool-operation :unite]}
                     {:key :intersect
                      :label "Intersect"
                      :action [:element/bool-operation :intersect]}
                     {:key :subtract
                      :label "Subtract"
                      :action [:element/bool-operation :subtract]}
                     {:key :divide
                      :label "Divide"
                      :action [:element/bool-operation :divide]}]}
            {:key :divider-5
             :type :separator}
            {:key :raise
             :label "Raise"
             :action [:element/raise]}
            {:key :lower
             :label "Lower"
             :action [:element/lower]}
            {:key :raise-to-top
             :label "Raise to top"
             :action [:element/raise-to-top]}
            {:key :lower-to-bottom
             :label "Lower to bottom"
             :action [:element/lower-to-bottom]}]}
   #_{:key :path
      :label "Path"
      :type :root
      :items [{:key :simplify
               :label "Simplify"
               :action [:element/manipulate-path :simplify]}
              {:key :smooth
               :label "Smooth"
               :action [:element/manipulate-path :smooth]}
              {:key :flatten
               :label "Flatten"
               :action [:element/manipulate-path :flatten]}
              {:key :reverse
               :label "Reverse"
               :action [:element/manipulate-path :reverse]}]}
   {:key :view
    :label "View"
    :type :root
    :items [{:key :zoom
             :label "Zoom"
             :type :sub-menu
             :items [{:key :zoom-in
                      :label "In"
                      :action [:zoom-in]}
                     {:key :zoom-out
                      :label "Out"
                      :action [:zoom-out]}
                     {:key :divider-1
                      :type :separator}
                     {:label "Set to 50%"
                      :key "50"
                      :action [:set-zoom 0.5]}
                     {:label "Set to 100%"
                      :key "100"
                      :action [:set-zoom 1]}
                     {:label "Set to 200%"
                      :key "200"
                      :action [:set-zoom 2]}
                     {:key :divider-1
                      :type :separator}
                     {:label "Initial"
                      :key "restore-active-page"
                      :action [:pan-to-active-page :original]}
                     {:label "Fit active page"
                      :key "fit-active-page"
                      :action [:pan-to-active-page :fit]}
                     {:label "Fill active page"
                      :key "fill-active-page"
                      :action [:pan-to-active-page :fill]}]}
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
             :action [:panel/toggle :repl-history]}
            {:key :toggle-debug-info
             :type :checkbox
             :label "Debug info"
             :checked? [:debug-info?]
             :action [:toggle-debug-info]}
            {:key :divider-3
             :type :separator}
            {:key :toggle-fullscreen
             :label "Fullscreen"
             :type :checkbox
             :checked? [:window/fullscreen?]
             :action [:window/toggle-fullscreen]}]}
   {:key :help
    :label "Help"
    :type :root
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

(defmulti menu-item :type)

(defmethod menu-item :separator
  []
  [:> Menubar/Separator {:class "menu-separator"}])

(defmethod menu-item :checkbox
  [{:keys [label action checked?]}]
  [:> Menubar/CheckboxItem
   {:class "menu-checkbox-item inset"
    :onSelect #(rf/dispatch action)
    :checked @(rf/subscribe checked?)}
   [:> Menubar/ItemIndicator
    {:class "menu-item-indicator"}
    [comp/icon "checkmark"]]
   label
   [:div.right-slot
    [comp/shortcuts action]]])

(defmethod menu-item :sub-menu
  [{:keys [label items]}]
  [:> Menubar/Sub
   [:> Menubar/SubTrigger
    {:class "sub-menu-item menu-item"}
    label
    [:div.right-slot.sub-menu-chevron
     [comp/icon "chevron-right" {:class "small"}]]]
   [:> Menubar/Portal
    (into [:> Menubar/SubContent
           {:class "menu-content"
            :align "start"
            :loop true}]
          (map menu-item items))]])

(defmethod menu-item :root
  [{:keys [label items key]}]
  [:> Menubar/Menu
   [:> Menubar/Trigger
    {:class "menubar-trigger"
     :id (name key)}
    label]
   [:> Menubar/Portal
    (into [:> Menubar/Content
           {:class "menu-content"
            :align "start"
            :loop true}]
          (map menu-item items))]])

(defmethod menu-item :default
  [{:keys [label action]}]
  [:> Menubar/Item
   {:class "menu-item"
    :onSelect #(rf/dispatch action)}
   label
   [:div.right-slot
    [comp/shortcuts action]]])

(defn root
  []
  [:> Menubar/Root
   {:class "menubar-root"
    :onValueChange #(rf/dispatch [:set-backdrop (seq %)])}
   (map (fn [item] ^{:key item} [menu-item item]) menu)
   [:button.button.px-3.flex.items-center
    {:on-click #(rf/dispatch [:cmdk/toggle])}
    "Command palette…"]])