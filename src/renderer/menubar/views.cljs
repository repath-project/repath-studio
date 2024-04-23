(ns renderer.menubar.views
  (:require
   ["@radix-ui/react-menubar" :as Menubar]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.menubar.filters :as filters]))

(defn recent-submenu
  []
  (let [recent @(rf/subscribe [:document/recent])
        recent-items (mapv (fn [path] {:key (keyword path)
                                       :label path
                                       :icon "folder"
                                       :action [:document/open path]}) recent)]
    (cond-> recent-items
      (seq recent-items)
      (concat [{:key :divider-1
                :type :separator}
               {:key :clear-recent
                :label "Clear recent"
                :icon "delete"
                :action [:document/clear-recent]}]))))

(defn file-menu
  []
  {:key :file
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
           {:key :recent
            :label "Recent"
            :type :sub-menu
            :disabled? [:document/recent-disabled?]
            :items (recent-submenu)}
           {:key :divider-2
            :type :separator}
           {:key :save
            :label "Save"
            :icon "save"
            :action [:document/save]
            :disabled? [:document/active-saved?]}
           {:key :save-as
            :label "Save as…"
            :icon "save"
            :action [:document/save-as]}
           {:key :download
            :icon "download"
            :label "Download"
            :action [:document/download]}
           {:key :divider-3
            :type :separator}
           {:key :export
            :label "Export as SVG"
            :icon "export"
            :action [:element/export]}
           {:key :divider-4
            :type :separator}
           {:key :close
            :label "Close"
            :icon "times"
            :action [:document/close-active]}
           {:key :exit
            :label "Exit"
            :icon "exit"
            :action [:window/close]}]})

(defn edit-menu
  []
  {:key :edit
   :label "Edit"
   :type :root
   :items [{:key :undo
            :label "Undo"
            :icon "undo"
            :action [:history/undo]}
           {:key :redo
            :label "Redo"
            :icon "redo"
            :action [:history/redo]}
           {:key :divider-1
            :type :separator}
           {:key :cut
            :label "Cut"
            :action [:element/cut]}
           {:key :copy
            :icon "copy"
            :label "Copy"
            :action [:element/copy]}
           {:key :paste
            :label "Paste"
            :icon "paste"
            :action [:element/paste]}
           {:key :paste-in-place
            :icon "paste"
            :label "Paste in place"
            :action [:element/paste-in-place]}
           {:key :paste-styles
            :icon "paste"
            :label "Paste styles"
            :action [:element/paste-styles]}
           {:key :divider-2
            :type :separator}
           {:key :duplicate
            :icon "copy"
            :label "Duplicate"
            :action [:element/duplicate-in-place]}
           {:key :divider-3
            :type :separator}
           {:key :select-all
            :icon "select-all"
            :label "Select all"
            :action [:element/select-all]}
           {:key :deselect-all
            :icon "deselect-all"
            :label "Deselect all"
            :action [:element/deselect-all]}
           {:key :invert-selection
            :label "Invert selection"
            :action [:element/invert-selection]}
           {:key :select-same-tags
            :icon "select-same"
            :label "Select same tags"
            :action [:element/select-same-tags]}
           {:key :divider-4
            :type :separator}
           {:key :delete
            :icon "delete"
            :label "Delete"
            :action [:element/delete]}]})

(defn align-submenu
  []
  [{:key :align-left
    :label "Left"
    :icon "objects-align-left"
    :action [:element/align :left]}
   {:key :align-center-horizontally
    :label "Center horizontally"
    :icon "objects-align-center-horizontal"
    :action [:element/align :center-horizontal]}
   {:key :align-right
    :label "Right"
    :icon "objects-align-right"
    :action [:element/align :right]}
   {:key :divider-1
    :type :separator}
   {:key :align-top
    :label "Top"
    :icon "objects-align-top"
    :action [:element/align :top]}
   {:key :align-center-vertically
    :label "Center vertically"
    :icon "objects-align-center-vertical"
    :action [:element/align :center-vertical]}
   {:key :align-bottom
    :label "Bottom"
    :icon "objects-align-bottom"
    :action [:element/align :bottom]}])

(defn boolean-submenu
  []
  [{:key :exclude
    :label "Exclude"
    :icon "exclude"
    :action [:element/bool-operation :exclude]}
   {:key :unite
    :label "Unite"
    :icon "unite"
    :action [:element/bool-operation :unite]}
   {:key :intersect
    :label "Intersect"
    :icon "intersect"
    :action [:element/bool-operation :intersect]}
   {:key :subtract
    :label "Subtract"
    :icon "subtract"
    :action [:element/bool-operation :subtract]}
   {:key :divide
    :label "Divide"
    :icon "divide"
    :action [:element/bool-operation :divide]}])

(defn path-submenu
  []
  [{:key :simplify
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
    :action [:element/manipulate-path :reverse]}])

(defn image-submenu
  []
  [{:key :trace
    :label "Trace"
    :action [:element/trace]}
   {:key :trianlgulate
    :label "Trianlgulate"
    :action [:element/triangulate]}])

(defn object-menu
  []
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
            :icon "group"
            :action [:element/group]}
           {:key :ungroup
            :label "Ungroup"
            :icon "ungroup"
            :action [:element/ungroup]}
           {:key :divider-2
            :type :separator}
           {:key :lock
            :label "Lock"
            :icon "lock"
            :action [:element/lock]}
           {:key :unlock
            :label "Unlock"
            :icon "unlock"
            :action [:element/unlock]}
           {:key :divider-3
            :type :separator}
           {:key :path
            :label "Align"
            :type :sub-menu
            :items (align-submenu)}
           {:key :divider-4
            :type :separator}
           {:key :boolean
            :label "Boolean operation"
            :type :sub-menu
            :items (boolean-submenu)}
           {:key :divider-5
            :type :separator}
           {:key :raise
            :label "Raise"
            :icon "bring-forward"
            :action [:element/raise]}
           {:key :lower
            :label "Lower"
            :icon "send-backward"
            :action [:element/lower]}
           {:key :raise-to-top
            :label "Raise to top"
            :icon "bring-front"
            :action [:element/raise-to-top]}
           {:key :lower-to-bottom
            :label "Lower to bottom"
            :icon "send-back"
            :action [:element/lower-to-bottom]}
           {:key :divider-6
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
    :action [:frame/zoom-in]}
   {:key :zoom-out
    :label "Out"
    :icon "zoom-out"
    :action [:frame/zoom-out]}
   {:key :divider-1
    :type :separator}
   {:label "Set to 50%"
    :key "50"
    :action [:frame/set-zoom 0.5]}
   {:label "Set to 100%"
    :key "100"
    :action [:frame/set-zoom 1]}
   {:label "Set to 200%"
    :key "200"
    :action [:frame/set-zoom 2]}
   {:key :divider-2
    :type :separator}
   {:label "Focus selected"
    :key "focus-selected"
    :action [:frame/focus-selection :original]}
   {:label "Fit selected"
    :key "fit-selected"
    :action [:frame/focus-selection :fit]}
   {:label "Fill selected"
    :key "fill-selected"
    :action [:frame/focus-selection :fill]}])

(defn a11y-submenu
  []
  (mapv (fn [{:keys [id]}]
          {:key id
           :label (name id)
           :type :checkbox
           :checked? [:document/filter-active? id]
           :action [:document/toggle-filter id]}) filters/accessibility))

(defn panels-submenu
  []
  [{:key :toggle-tree
    :type :checkbox
    :label "Element tree"
    :checked? [:panel/visible? :tree]
    :action [:panel/toggle :tree]}
   {:key :toggle-props
    :type :checkbox
    :label "Properties"
    :checked? [:panel/visible? :properties]
    :action [:panel/toggle :properties]}
   {:key :toggle-xml
    :label "XML view"
    :type :checkbox
    :icon "code"
    :checked? [:panel/visible? :xml]
    :action [:panel/toggle :xml]}
   {:key :toggle-history
    :label "History tree"
    :icon "history"
    :type :checkbox
    :checked? [:panel/visible? :history]
    :action [:panel/toggle :history]}
   {:key :toggle-command-history
    :type :checkbox
    :label "Shell history"
    :icon "shell"
    :checked? [:panel/visible? :repl-history]
    :action [:panel/toggle :repl-history]}
   {:key :toggle-timeline-panel
    :type :checkbox
    :label "Timeline editor"
    :icon "timeline"
    :checked? [:panel/visible? :timeline]
    :action [:panel/toggle :timeline]}
   {:key :divider-2
    :type :separator}])

(defn view-menu
  []
  {:key :view
   :label "View"
   :type :root
   :items [{:key :zoom
            :label "Zoom"
            :type :sub-menu
            :items (zoom-submenu)}
           {:key :a11y
            :label "Accessibility filters"
            :type :sub-menu
            :items (a11y-submenu)}
           {:key :divider-1
            :type :separator}
           {:key :toggle-grid
            :type :checkbox
            :label "Grid"
            :icon "grid"
            :checked? [:grid?]
            :action [:toggle-grid]}
           {:key :toggle-rulers
            :type :checkbox
            :label "Rulers"
            :icon "ruler-combined"
            :checked? [:rulers?]
            :action [:toggle-rulers]}
           {:key :toggle-debug-info
            :type :checkbox
            :label "Debug info"
            :checked? [:debug-info?]
            :action [:toggle-debug-info]}
           {:key :divider-2
            :type :separator}
           {:key :panels
            :label "Panels"
            :type :sub-menu
            :items (panels-submenu)}
           {:key :divider-3
            :type :separator}
           {:key :toggle-fullscreen
            :label "Fullscreen"
            :icon "arrow-minimize"
            :type :checkbox
            :checked? [:window/fullscreen?]
            :action [:window/toggle-fullscreen]}]})

(defn help-menu
  []
  {:key :help
   :label "Help"
   :type :root
   :items [{:key :cmdk
            :label "Command panel"
            :action [:dialog/cmdk]}
           {:key :divider-1
            :type :separator}
           {:key :website
            :label "Website"
            :action [:window/open-remote-url "https://repath.studio/"]}
           {:key :source-code
            :label "Source Code"
            :action [:window/open-remote-url "https://github.com/re-path/studio"]}
           {:key :license
            :label "License"
            :action [:window/open-remote-url "https://github.com/re-path/studio/blob/main/LICENSE"]}
           {:key :changelog
            :label "Changelog"
            :action [:window/open-remote-url "https://repath.studio/roadmap/changelog/"]}
           {:key :divider-2
            :type :separator}
           {:key :submit-issue
            :label "Submit an issue"
            :action [:window/open-remote-url "https://github.com/re-path/studio/issues/new/choose"]}
           {:key :divider-3
            :type :separator}
           {:key :about
            :icon "info"
            :label "About"
            :action [:dialog/about]}]})

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
     :disabled (when disabled? @(rf/subscribe disabled?))}
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
  [{:keys [label items key action]}]
  [:> Menubar/Menu
   [:> Menubar/Trigger
    {:class "menubar-trigger"
     :id (name key)
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
                   (rf/dispatch [:menubar/focus nil]))
    :disabled (when disabled? @(rf/subscribe disabled?))}
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
