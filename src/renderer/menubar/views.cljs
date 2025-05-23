(ns renderer.menubar.views
  (:require
   ["@radix-ui/react-menubar" :as Menubar]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.events :as-alias element.events]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.events :as-alias frame.events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]
   [renderer.menubar.events :as-alias menubar.events]
   [renderer.menubar.filters :as filters]
   [renderer.ruler.events :as-alias ruler.events]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.ui :as ui]
   [renderer.window.events :as-alias window.events]
   [renderer.window.subs :as-alias window.subs]))

(defn recent-submenu
  []
  (let [recent @(rf/subscribe [::document.subs/recent])
        recent-items (mapv (fn [path] {:id (keyword path)
                                       :label path
                                       :icon "folder"
                                       :action [::document.events/open path]}) recent)]
    (cond-> recent-items
      (seq recent-items)
      (concat [{:id :divider-1
                :type :separator}
               {:id :clear-recent
                :label "Clear recent"
                :icon "delete"
                :action [::document.events/clear-recent]}]))))

(defn file-menu
  []
  {:id :file
   :label "File"
   :type :root
   :items [{:id :new-file
            :label "New"
            :icon "file"
            :action [::document.events/new]}
           {:id :divider-1
            :type :separator}
           {:id :open-file
            :label "Open…"
            :icon "folder"
            :action [::document.events/open nil]}
           {:id :recent
            :label "Recent"
            :type :sub-menu
            :disabled (not @(rf/subscribe [::document.subs/recent?]))
            :items (recent-submenu)}
           {:id :divider-2
            :type :separator}
           {:id :save
            :label "Save"
            :icon "save"
            :action [::document.events/save]
            :disabled (or (not @(rf/subscribe [::document.subs/entities?]))
                          @(rf/subscribe [::document.subs/active-saved?]))}
           {:id :save-as
            :label "Save as…"
            :icon "save-as"
            :action [::document.events/save-as]
            :disabled (not @(rf/subscribe [::document.subs/entities?]))}
           {:id :download
            :icon "download"
            :label "Download"
            :disabled (not @(rf/subscribe [::document.subs/entities?]))
            :action [::document.events/download]}
           {:id :divider-3
            :type :separator}
           {:id :export-svg
            :label "Export as SVG"
            :icon "export"
            :disabled (not @(rf/subscribe [::document.subs/entities?]))
            :action [::element.events/export-svg]}
           {:id :divider-4
            :type :separator}
           {:id :print
            :label "Print"
            :icon "printer"
            :disabled (not @(rf/subscribe [::document.subs/entities?]))
            :action [::element.events/print]}
           {:id :divider-5
            :type :separator}
           {:id :close
            :label "Close"
            :icon "window-close"
            :disabled (not @(rf/subscribe [::document.subs/entities?]))
            :action [::document.events/close-active]}
           {:id :exit
            :label "Exit"
            :icon "exit"
            :action [::window.events/close]}]})

(defn edit-menu
  []
  {:id :edit
   :label "Edit"
   :type :root
   :disabled (not @(rf/subscribe [::document.subs/entities?]))
   :items [{:id :undo
            :label "Undo"
            :icon "undo"
            :disabled (not @(rf/subscribe [::history.subs/undos?]))
            :action [::history.events/undo]}
           {:id :redo
            :label "Redo"
            :icon "redo"
            :disabled (not @(rf/subscribe [::history.subs/redos?]))
            :action [::history.events/redo]}
           {:id :divider-1
            :type :separator}
           {:id :cut
            :label "Cut"
            :icon "cut"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/cut]}
           {:id :copy
            :icon "copy"
            :label "Copy"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/copy]}
           {:id :paste
            :label "Paste"
            :icon "paste"
            :action [::element.events/paste]}
           {:id :paste-in-place
            :icon "paste"
            :label "Paste in place"
            :action [::element.events/paste-in-place]}
           {:id :paste-styles
            :icon "paste"
            :label "Paste styles"
            :action [::element.events/paste-styles]}
           {:id :divider-2
            :type :separator}
           {:id :duplicate
            :icon "copy"
            :label "Duplicate"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/duplicate]}
           {:id :divider-3
            :type :separator}
           {:id :select-all
            :icon "select-all"
            :label "Select all"
            :action [::element.events/select-all]}
           {:id :deselect-all
            :icon "deselect-all"
            :label "Deselect all"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/deselect-all]}
           {:id :invert-selection
            :label "Invert selection"
            :icon "invert-selection"
            :action [::element.events/invert-selection]}
           {:id :select-same-tags
            :icon "select-same"
            :label "Select same tags"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/select-same-tags]}
           {:id :divider-4
            :type :separator}
           {:id :delete
            :icon "delete"
            :label "Delete"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/delete]}]})

(defn align-submenu
  []
  [{:id :align-left
    :label "Left"
    :icon "objects-align-left"
    :disabled @(rf/subscribe [::element.subs/every-top-level])
    :action [::element.events/align :left]}
   {:id :align-center-horizontally
    :label "Center horizontally"
    :icon "objects-align-center-horizontal"
    :action [::element.events/align :center-horizontal]}
   {:id :align-right
    :label "Right"
    :icon "objects-align-right"
    :action [::element.events/align :right]}
   {:id :divider-1
    :type :separator}
   {:id :align-top
    :label "Top"
    :icon "objects-align-top"
    :action [::element.events/align :top]}
   {:id :align-center-vertically
    :label "Center vertically"
    :icon "objects-align-center-vertical"
    :action [::element.events/align :center-vertical]}
   {:id :align-bottom
    :label "Bottom"
    :icon "objects-align-bottom"
    :action [::element.events/align :bottom]}])

(defn boolean-submenu
  []
  [{:id :exclude
    :label "Exclude"
    :icon "exclude"
    :action [::element.events/boolean-operation :exclude]}
   {:id :unite
    :label "Unite"
    :icon "unite"
    :action [::element.events/boolean-operation :unite]}
   {:id :intersect
    :label "Intersect"
    :icon "intersect"
    :action [::element.events/boolean-operation :intersect]}
   {:id :subtract
    :label "Subtract"
    :icon "subtract"
    :action [::element.events/boolean-operation :subtract]}
   {:id :divide
    :label "Divide"
    :icon "divide"
    :action [::element.events/boolean-operation :divide]}])

(defn animate-submenu
  []
  [{:id :animate
    :label "Animate"
    :icon "animation"
    :action [::element.events/animate :animate {}]}
   {:id :animate-transform
    :label "Animate Transform"
    :icon "animation"
    :action [::element.events/animate :animateTransform {}]}
   {:id :animate-motion
    :icon "animation"
    :label "Animate Motion"
    :action [::element.events/animate :animateMotion {}]}])

(defn path-submenu
  []
  [{:id :simplify
    :label "Simplify"
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :simplify]}
   {:id :smooth
    :label "Smooth"
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :smooth]}
   {:id :flatten
    :label "Flatten"
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :flatten]}
   {:id :reverse
    :label "Reverse"
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :reverse]}])

(defn image-submenu
  []
  [{:id :trace
    :label "Trace"
    :icon "image"
    :action [::element.events/trace]}])

(defn object-menu
  []
  {:id :object
   :label "Object"
   :type :root
   :disabled (not @(rf/subscribe [::document.subs/entities?]))
   :items [{:id :to-path
            :label "Object to path"
            :icon "bezier-curve"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/->path]}
           {:id :stroke-to-path
            :label "Stroke to path"
            :icon "bezier-curve"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/stroke->path]}
           {:id :divider-1
            :type :separator}
           {:id :group
            :label "Group"
            :icon "group"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/group]}
           {:id :ungroup
            :label "Ungroup"
            :icon "ungroup"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/ungroup]}
           {:id :divider-2
            :type :separator}
           {:id :lock
            :label "Lock"
            :icon "lock"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/lock]}
           {:id :unlock
            :label "Unlock"
            :icon "unlock"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/unlock]}
           {:id :divider-3
            :type :separator}
           {:id :path
            :label "Align"
            :type :sub-menu
            :disabled @(rf/subscribe [::element.subs/every-top-level])
            :items (align-submenu)}
           {:id :boolean
            :label "Animate"
            :type :sub-menu
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :items (animate-submenu)}
           {:id :boolean
            :label "Boolean operation"
            :type :sub-menu
            :disabled (not @(rf/subscribe [::element.subs/multiple-selected?]))
            :items (boolean-submenu)}
           {:id :divider-4
            :type :separator}
           {:id :raise
            :label "Raise"
            :icon "bring-forward"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/raise]}
           {:id :lower
            :label "Lower"
            :icon "send-backward"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/lower]}
           {:id :raise-to-top
            :label "Raise to top"
            :icon "bring-front"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/raise-to-top]}
           {:id :lower-to-bottom
            :label "Lower to bottom"
            :icon "send-back"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/lower-to-bottom]}
           {:id :divider-5
            :type :separator}
           {:id :image
            :type :sub-menu
            :label "Image"
            :items (image-submenu)}
           {:id :path
            :label "Path"
            :type :sub-menu
            :items (path-submenu)}]})

(defn zoom-submenu
  []
  [{:id :zoom-in
    :label "In"
    :icon "zoom-in"
    :action [::frame.events/zoom-in]}
   {:id :zoom-out
    :label "Out"
    :icon "zoom-out"
    :action [::frame.events/zoom-out]}
   {:id :divider-1
    :type :separator}
   {:label "Set to 50%"
    :id "50"
    :icon "magnifier"
    :action [::frame.events/set-zoom 0.5]}
   {:label "Set to 100%"
    :id "100"
    :icon "magnifier"
    :action [::frame.events/set-zoom 1]}
   {:label "Set to 200%"
    :id "200"
    :icon "magnifier"
    :action [::frame.events/set-zoom 2]}
   {:id :divider-2
    :type :separator}
   {:label "Focus selected"
    :id "focus-selected"
    :icon "focus"
    :action [::frame.events/focus-selection :original]}
   {:label "Fit selected"
    :id "fit-selected"
    :icon "focus"
    :action [::frame.events/focus-selection :fit]}
   {:label "Fill selected"
    :id "fill-selected"
    :icon "focus"
    :action [::frame.events/focus-selection :fill]}])

(defn a11y-submenu
  []
  (mapv (fn [{:keys [id]}]
          {:id id
           :label (name id)
           :type :checkbox
           :icon "a11y"
           :checked @(rf/subscribe [::document.subs/filter-active id])
           :action [::document.events/toggle-filter id]}) filters/accessibility))

(defn panel-submenu
  []
  [{:id :toggle-tree
    :type :checkbox
    :icon "tree"
    :label "Element tree"
    :checked @(rf/subscribe [::app.subs/panel-visible? :tree])
    :action [::app.events/toggle-panel :tree]}
   {:id :toggle-props
    :type :checkbox
    :icon "properties"
    :label "Properties"
    :checked @(rf/subscribe [::app.subs/panel-visible? :properties])
    :action [::app.events/toggle-panel :properties]}
   {:id :toggle-xml
    :label "XML view"
    :type :checkbox
    :icon "code"
    :checked @(rf/subscribe [::app.subs/panel-visible? :xml])
    :action [::app.events/toggle-panel :xml]}
   {:id :toggle-history
    :label "History tree"
    :icon "history"
    :type :checkbox
    :checked @(rf/subscribe [::app.subs/panel-visible? :history])
    :action [::app.events/toggle-panel :history]}
   {:id :toggle-command-history
    :type :checkbox
    :label "Shell history"
    :icon "shell"
    :checked @(rf/subscribe [::app.subs/panel-visible? :repl-history])
    :action [::app.events/toggle-panel :repl-history]}
   {:id :toggle-timeline-panel
    :type :checkbox
    :label "Timeline editor"
    :icon "timeline"
    :checked @(rf/subscribe [::app.subs/panel-visible? :timeline])
    :action [::app.events/toggle-panel :timeline]}
   {:id :divider-2
    :type :separator}])

(defn view-menu
  []
  {:id :view
   :label "View"
   :type :root
   :disabled (not @(rf/subscribe [::document.subs/entities?]))
   :items [{:id :zoom
            :label "Zoom"
            :type :sub-menu
            :items (zoom-submenu)}
           {:id :a11y
            :label "Accessibility filter"
            :type :sub-menu
            :items (a11y-submenu)}
           {:id :divider-1
            :type :separator}
           {:id :toggle-grid
            :type :checkbox
            :label "Grid"
            :icon "grid"
            :checked @(rf/subscribe [::app.subs/grid])
            :action [::app.events/toggle-grid]}
           {:id :toggle-ruler
            :type :checkbox
            :label "Rulers"
            :icon "ruler-combined"
            :checked @(rf/subscribe [::ruler.subs/visible?])
            :action [::ruler.events/toggle-visible]}
           {:id :help-bar
            :type :checkbox
            :label "Help bar"
            :icon "info"
            :checked @(rf/subscribe [::app.subs/help-bar])
            :action [::app.events/toggle-help-bar]}
           {:id :toggle-debug-info
            :type :checkbox
            :label "Debug info"
            :icon "bug"
            :checked @(rf/subscribe [::app.subs/debug-info])
            :action [::app.events/toggle-debug-info]}
           {:id :divider-2
            :type :separator}
           {:id :panel
            :label "Panel"
            :type :sub-menu
            :items (panel-submenu)}
           {:id :divider-3
            :type :separator}
           {:id :toggle-fullscreen
            :label "Fullscreen"
            :icon "arrow-minimize"
            :type :checkbox
            :checked @(rf/subscribe [::window.subs/fullscreen?])
            :action [::window.events/toggle-fullscreen]}]})

(defn help-menu
  []
  {:id :help
   :label "Help"
   :type :root
   :items [{:id :cmdk
            :label "Command panel"
            :icon "command"
            :action [::dialog.events/cmdk]}
           {:id :divider-1
            :type :separator}
           {:id :website
            :label "Website"
            :icon "earth"
            :action [::window.events/open-remote-url
                     "https://repath.studio/"]}
           {:id :source-code
            :label "Source Code"
            :icon "commit"
            :action [::window.events/open-remote-url
                     "https://github.com/repath-project/repath-studio"]}
           {:id :license
            :label "License"
            :icon "lgpl"
            :action [::window.events/open-remote-url
                     "https://github.com/repath-project/repath-studio/blob/main/LICENSE"]}
           {:id :changelog
            :icon "list"
            :label "Changelog"
            :action [::window.events/open-remote-url
                     "https://repath.studio/roadmap/changelog/"]}
           {:id :divider-2
            :type :separator}
           {:id :submit-issue
            :icon "warning"
            :label "Submit an issue"
            :action [::window.events/open-remote-url
                     "https://github.com/repath-project/repath-studio/issues/new/choose"]}
           {:id :divider-3
            :type :separator}
           {:id :about
            :icon "info"
            :label "About"
            :action [::dialog.events/about]}]})

(defmulti menu-item :type)

(defmethod menu-item :separator
  []
  [:> Menubar/Separator {:class "menu-separator"}])

(defmethod menu-item :checkbox
  [{:keys [label action checked]}]
  [:> Menubar/CheckboxItem
   {:class "menu-checkbox-item inset"
    :on-select #(rf/dispatch action)
    :checked checked}
   [:> Menubar/ItemIndicator
    {:class "menu-item-indicator"}
    [ui/icon "checkmark"]]
   label
   [:div.right-slot
    [ui/shortcuts action]]])

(defmethod menu-item :sub-menu
  [{:keys [label items disabled]}]
  [:> Menubar/Sub
   [:> Menubar/SubTrigger
    {:class "sub-menu-item menu-item"
     :disabled disabled}
    label
    [:div.right-slot.sub-menu-chevron
     [ui/icon "chevron-right"]]]
   [:> Menubar/Portal
    (into [:> Menubar/SubContent
           {:class "menu-content"
            :align "start"
            :loop true}]
          (map menu-item items))]])

(defmethod menu-item :root
  [{:keys [label items id disabled]}]
  [:> Menubar/Menu
   [:> Menubar/Trigger
    {:class "menubar-trigger"
     :id (name id)
     :disabled disabled}
    label]
   [:> Menubar/Portal
    (into [:> Menubar/Content
           {:class (when items "menu-content")
            :align "start"
            :side-offset 3
            :loop true}]
          (map menu-item items))]])

(defmethod menu-item :default
  [{:keys [label action disabled]}]
  [:> Menubar/Item
   {:class "menu-item"
    :on-select #(rf/dispatch [::menubar.events/select-item action])
    :disabled disabled}
   label
   [:div.right-slot
    [ui/shortcuts action]]])

(defn submenus
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
          :on-key-down #(.stopPropagation %) ; FIXME: Esc global action also triggered.
          :on-value-change #(rf/dispatch [::app.events/set-backdrop (boolean (seq %))])}]
        (map menu-item (submenus))))
