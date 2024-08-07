(ns renderer.menubar.views
  (:require
   ["@radix-ui/react-menubar" :as Menubar]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.menubar.filters :as filters]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.events :as-alias element.e]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.events :as-alias frame.e]
   [renderer.history.events :as-alias history.e]
   [renderer.history.subs :as-alias history.s]
   [renderer.window.events :as-alias window.e]
   [renderer.window.subs :as-alias window.s]))

(defn recent-submenu
  []
  (let [recent @(rf/subscribe [::document.s/recent])
        recent-items (mapv (fn [path] {:key (keyword path)
                                       :label path
                                       :icon "folder"
                                       :action [::document.e/open path]}) recent)]
    (cond-> recent-items
      (seq recent-items)
      (concat [{:key :divider-1
                :type :separator}
               {:key :clear-recent
                :label "Clear recent"
                :icon "delete"
                :action [::document.e/clear-recent]}]))))

(defn file-menu
  []
  {:key :file
   :label "File"
   :type :root
   :items [{:key :new-file
            :label "New"
            :icon "file"
            :action [::document.e/new]}
           {:key :divider-1
            :type :separator}
           {:key :open-file
            :label "Open…"
            :icon "folder"
            :action [::document.e/open]}
           {:key :recent
            :label "Recent"
            :type :sub-menu
            :disabled? (not @(rf/subscribe [::document.s/recent?]))
            :items (recent-submenu)}
           {:key :divider-2
            :type :separator}
           {:key :save
            :label "Save"
            :icon "save"
            :action [::document.e/save]
            :disabled? (or (not @(rf/subscribe [::document.s/documents?]))
                           @(rf/subscribe [::document.s/active-saved?]))}
           {:key :save-as
            :label "Save as…"
            :icon "save-as"
            :action [::document.e/save-as]
            :disabled? (not @(rf/subscribe [::document.s/documents?]))}
           {:key :download
            :icon "download"
            :label "Download"
            :disabled? (not @(rf/subscribe [::document.s/documents?]))
            :action [::document.e/download]}
           {:key :divider-3
            :type :separator}
           {:key :export-svg
            :label "Export as SVG"
            :icon "export"
            :disabled? (not @(rf/subscribe [::document.s/documents?]))
            :action [::element.e/export-svg]}
           {:key :divider-4
            :type :separator}
           {:key :close
            :label "Close"
            :icon "window-close"
            :disabled? (not @(rf/subscribe [::document.s/documents?]))
            :action [::document.e/close-active]}
           {:key :exit
            :label "Exit"
            :icon "exit"
            :action [::window.e/close]}]})

(defn edit-menu
  []
  {:key :edit
   :label "Edit"
   :type :root
   :disabled? (not @(rf/subscribe [::document.s/documents?]))
   :items [{:key :undo
            :label "Undo"
            :icon "undo"
            :disabled? (not @(rf/subscribe [::history.s/undos?]))
            :action [::history.e/undo]}
           {:key :redo
            :label "Redo"
            :icon "redo"
            :disabled? (not @(rf/subscribe [::history.s/redos?]))
            :action [::history.e/redo]}
           {:key :divider-1
            :type :separator}
           {:key :cut
            :label "Cut"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/cut]}
           {:key :copy
            :icon "copy"
            :label "Copy"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/copy]}
           {:key :paste
            :label "Paste"
            :icon "paste"
            :action [::element.e/paste]}
           {:key :paste-in-place
            :icon "paste"
            :label "Paste in place"
            :action [::element.e/paste-in-place]}
           {:key :paste-styles
            :icon "paste"
            :label "Paste styles"
            :action [::element.e/paste-styles]}
           {:key :divider-2
            :type :separator}
           {:key :duplicate
            :icon "copy"
            :label "Duplicate"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/duplicate-in-place]}
           {:key :divider-3
            :type :separator}
           {:key :select-all
            :icon "select-all"
            :label "Select all"
            :action [::element.e/select-all]}
           {:key :deselect-all
            :icon "deselect-all"
            :label "Deselect all"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/deselect-all]}
           {:key :invert-selection
            :label "Invert selection"
            :action [::element.e/invert-selection]}
           {:key :select-same-tags
            :icon "select-same"
            :label "Select same tags"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/select-same-tags]}
           {:key :divider-4
            :type :separator}
           {:key :delete
            :icon "delete"
            :label "Delete"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/delete]}]})

(defn align-submenu
  []
  [{:key :align-left
    :label "Left"
    :icon "objects-align-left"
    :disabled @(rf/subscribe [::element.s/top-level?])
    :action [::element.e/align :left]}
   {:key :align-center-horizontally
    :label "Center horizontally"
    :icon "objects-align-center-horizontal"
    :action [::element.e/align :center-horizontal]}
   {:key :align-right
    :label "Right"
    :icon "objects-align-right"
    :action [::element.e/align :right]}
   {:key :divider-1
    :type :separator}
   {:key :align-top
    :label "Top"
    :icon "objects-align-top"
    :action [::element.e/align :top]}
   {:key :align-center-vertically
    :label "Center vertically"
    :icon "objects-align-center-vertical"
    :action [::element.e/align :center-vertical]}
   {:key :align-bottom
    :label "Bottom"
    :icon "objects-align-bottom"
    :action [::element.e/align :bottom]}])

(defn boolean-submenu
  []
  [{:key :exclude
    :label "Exclude"
    :icon "exclude"
    :action [::element.e/bool-operation :exclude]}
   {:key :unite
    :label "Unite"
    :icon "unite"
    :action [::element.e/bool-operation :unite]}
   {:key :intersect
    :label "Intersect"
    :icon "intersect"
    :action [::element.e/bool-operation :intersect]}
   {:key :subtract
    :label "Subtract"
    :icon "subtract"
    :action [::element.e/bool-operation :subtract]}
   {:key :divide
    :label "Divide"
    :icon "divide"
    :action [::element.e/bool-operation :divide]}])

(defn animate-submenu
  []
  [{:key :animate
    :label "Animate"
    :action [::element.e/animate :animate {}]}
   {:key :animate-transform
    :label "Animate Transform"
    :action [::element.e/animate :animateTransform {}]}
   {:key :animate-motion
    :label "Animate Motion"
    :action [::element.e/animate :animateMotion {}]}])

(defn path-submenu
  []
  [{:key :simplify
    :label "Simplify"
    :action [::element.e/manipulate-path :simplify]}
   {:key :smooth
    :label "Smooth"
    :action [::element.e/manipulate-path :smooth]}
   {:key :flatten
    :label "Flatten"
    :action [::element.e/manipulate-path :flatten]}
   {:key :reverse
    :label "Reverse"
    :action [::element.e/manipulate-path :reverse]}])

(defn image-submenu
  []
  [{:key :trace
    :label "Trace"
    :action [::element.e/trace]}])

(defn object-menu
  []
  {:key :object
   :label "Object"
   :type :root
   :disabled? (not @(rf/subscribe [::document.s/documents?]))
   :items [{:key :to-path
            :label "Object to path"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/->path]}
           {:key :stroke-to-path
            :label "Stroke to path"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/stroke->path]}
           {:key :divider-1
            :type :separator}
           {:key :group
            :label "Group"
            :icon "group"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/group]}
           {:key :ungroup
            :label "Ungroup"
            :icon "ungroup"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/ungroup]}
           {:key :divider-2
            :type :separator}
           {:key :lock
            :label "Lock"
            :icon "lock"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/lock]}
           {:key :unlock
            :label "Unlock"
            :icon "unlock"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/unlock]}
           {:key :divider-3
            :type :separator}
           {:key :path
            :label "Align"
            :type :sub-menu
            :disabled? @(rf/subscribe [::element.s/top-level?])
            :items (align-submenu)}
           {:key :boolean
            :label "Animate"
            :type :sub-menu
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :items (animate-submenu)}
           {:key :boolean
            :label "Boolean operation"
            :type :sub-menu
            :disabled? (not @(rf/subscribe [::element.s/multiple-selected?]))
            :items (boolean-submenu)}
           {:key :divider-4
            :type :separator}
           {:key :raise
            :label "Raise"
            :icon "bring-forward"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/raise]}
           {:key :lower
            :label "Lower"
            :icon "send-backward"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/lower]}
           {:key :raise-to-top
            :label "Raise to top"
            :icon "bring-front"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/raise-to-top]}
           {:key :lower-to-bottom
            :label "Lower to bottom"
            :icon "send-back"
            :disabled? (not @(rf/subscribe [::element.s/selected?]))
            :action [::element.e/lower-to-bottom]}
           {:key :divider-5
            :type :separator}
           {:key :image
            :type :sub-menu
            :label "Image"
            :items (image-submenu)}
           {:key :path
            :label "Path"
            :type :sub-menu
            :items (path-submenu)}]})

(defn zoom-submenu
  []
  [{:key :zoom-in
    :label "In"
    :icon "zoom-in"
    :action [::frame.e/zoom-in]}
   {:key :zoom-out
    :label "Out"
    :icon "zoom-out"
    :action [::frame.e/zoom-out]}
   {:key :divider-1
    :type :separator}
   {:label "Set to 50%"
    :key "50"
    :action [::frame.e/set-zoom 0.5]}
   {:label "Set to 100%"
    :key "100"
    :action [::frame.e/set-zoom 1]}
   {:label "Set to 200%"
    :key "200"
    :action [::frame.e/set-zoom 2]}
   {:key :divider-2
    :type :separator}
   {:label "Focus selected"
    :key "focus-selected"
    :action [::frame.e/focus-selection :original]}
   {:label "Fit selected"
    :key "fit-selected"
    :action [::frame.e/focus-selection :fit]}
   {:label "Fill selected"
    :key "fill-selected"
    :action [::frame.e/focus-selection :fill]}])

(defn a11y-submenu
  []
  (mapv (fn [{:keys [id]}]
          {:key id
           :label (name id)
           :type :checkbox
           :checked? [::document.s/filter-active? id]
           :action [::document.e/toggle-filter id]}) filters/accessibility))

(defn panel-submenu
  []
  [{:key :toggle-tree
    :type :checkbox
    :icon "tree"
    :label "Element tree"
    :checked? [:panel-visible? :tree]
    :action [:toggle-panel :tree]}
   {:key :toggle-props
    :type :checkbox
    :icon "properties"
    :label "Properties"
    :checked? [:panel-visible? :properties]
    :action [:toggle-panel :properties]}
   {:key :toggle-xml
    :label "XML view"
    :type :checkbox
    :icon "code"
    :checked? [:panel-visible? :xml]
    :action [:toggle-panel :xml]}
   {:key :toggle-history
    :label "History tree"
    :icon "history"
    :type :checkbox
    :checked? [:panel-visible? :history]
    :action [:toggle-panel :history]}
   {:key :toggle-command-history
    :type :checkbox
    :label "Shell history"
    :icon "shell"
    :checked? [:panel-visible? :repl-history]
    :action [:toggle-panel :repl-history]}
   {:key :toggle-timeline-panel
    :type :checkbox
    :label "Timeline editor"
    :icon "timeline"
    :checked? [:panel-visible? :timeline]
    :action [:toggle-panel :timeline]}
   {:key :divider-2
    :type :separator}])

(defn view-menu
  []
  {:key :view
   :label "View"
   :type :root
   :disabled? (not @(rf/subscribe [::document.s/documents?]))
   :items [{:key :zoom
            :label "Zoom"
            :type :sub-menu
            :items (zoom-submenu)}
           {:key :a11y
            :label "Accessibility filter"
            :type :sub-menu
            :items (a11y-submenu)}
           {:key :divider-1
            :type :separator}
           {:key :toggle-grid
            :type :checkbox
            :label "Grid"
            :icon "grid"
            :checked? [:grid-visible?]
            :action [:toggle-grid]}
           {:key :toggle-rulers
            :type :checkbox
            :label "Rulers"
            :icon "ruler-combined"
            :checked? [:rulers-visible?]
            :action [:toggle-rulers]}
           {:key :toggle-debug-info
            :type :checkbox
            :label "Debug info"
            :checked? [:debug-info?]
            :action [:toggle-debug-info]}
           {:key :divider-2
            :type :separator}
           {:key :panel
            :label "Panel"
            :type :sub-menu
            :items (panel-submenu)}
           {:key :divider-3
            :type :separator}
           {:key :toggle-fullscreen
            :label "Fullscreen"
            :icon "arrow-minimize"
            :type :checkbox
            :checked? [::window.s/fullscreen?]
            :action [::window.e/toggle-fullscreen]}]})

(defn help-menu
  []
  {:key :help
   :label "Help"
   :type :root
   :items [{:key :cmdk
            :label "Command panel"
            :icon "command"
            :action [::dialog.e/cmdk]}
           {:key :divider-1
            :type :separator}
           {:key :website
            :label "Website"
            :icon "earth"
            :action [::window.e/open-remote-url "https://repath.studio/"]}
           {:key :source-code
            :label "Source Code"
            :icon "commit"
            :action [::window.e/open-remote-url "https://github.com/repath-project/repath-studio"]}
           {:key :license
            :label "License"
            :action [::window.e/open-remote-url "https://github.com/repath-project/repath-studio/blob/main/LICENSE"]}
           {:key :changelog
            :icon "list"
            :label "Changelog"
            :action [::window.e/open-remote-url "https://repath.studio/roadmap/changelog/"]}
           {:key :divider-2
            :type :separator}
           {:key :submit-issue
            :icon "warning"
            :label "Submit an issue"
            :action [::window.e/open-remote-url "https://github.com/repath-project/repath-studio/issues/new/choose"]}
           {:key :divider-3
            :type :separator}
           {:key :about
            :icon "info"
            :label "About"
            :action [::dialog.e/about]}]})

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
  [{:keys [label items disabled?]}]
  [:> Menubar/Sub
   [:> Menubar/SubTrigger
    {:class "sub-menu-item menu-item"
     :disabled disabled?}
    label
    [:div.right-slot.sub-menu-chevron
     [comp/icon "chevron-right" {:class "icon small"}]]]
   [:> Menubar/Portal
    (into [:> Menubar/SubContent
           {:class "menu-content"
            :align "start"
            :loop true}]
          (map menu-item items))]])

(defmethod menu-item :root
  [{:keys [label items key action disabled?]}]
  [:> Menubar/Menu
   [:> Menubar/Trigger
    {:class "menubar-trigger"
     :id (name key)
     :disabled disabled?
     :on-click (when action #(rf/dispatch action))
     :on-key-down (fn [e]
                    ; FIXME: Doesn't work when the menu content is open.
                    (when (contains? #{"Enter" "Space"} (.-key e))
                      (rf/dispatch action)))}
    label]
   [:> Menubar/Portal
    (into [:> Menubar/Content
           {:class (when items "menu-content")
            :align "start"
            :sideOffset 3
            :loop true}]
          (map menu-item items))]])

(defmethod menu-item :default
  [{:keys [label action disabled?]}]
  [:> Menubar/Item
   {:class "menu-item"
    :onSelect #(do (rf/dispatch action)
                   (rf/dispatch [:focus nil]))
    :disabled disabled?}
   label
   [:div.right-slot
    [comp/shortcuts action]]])

(defn root-menu
  []
  [(file-menu)
   (edit-menu)
   (object-menu)
   (view-menu)
   (help-menu)])

(defn root
  []
  (into [:> Menubar/Root
         {:class "menubar-root"
          :on-key-down #(when-not (= (.-key %) "Escape")
                          (.stopPropagation %)) ; FIXME: Esc global action also triggered.
          :onValueChange #(rf/dispatch [:set-backdrop (seq %)])}]
        (map menu-item (root-menu))))
