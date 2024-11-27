(ns renderer.menubar.views
  (:require
   ["@radix-ui/react-menubar" :as Menubar]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.app.subs :as-alias app.s]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.events :as-alias element.e]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.events :as-alias frame.e]
   [renderer.history.events :as-alias history.e]
   [renderer.history.subs :as-alias history.s]
   [renderer.menubar.events :as-alias e]
   [renderer.menubar.filters :as filters]
   [renderer.ruler.events :as-alias ruler.e]
   [renderer.ruler.subs :as-alias ruler.s]
   [renderer.ui :as ui]
   [renderer.window.events :as-alias window.e]
   [renderer.window.subs :as-alias window.s]))

(defn recent-submenu
  []
  (let [recent @(rf/subscribe [::document.s/recent])
        recent-items (mapv (fn [path] {:id (keyword path)
                                       :label path
                                       :icon "folder"
                                       :action [::document.e/open path]}) recent)]
    (cond-> recent-items
      (seq recent-items)
      (concat [{:id :divider-1
                :type :separator}
               {:id :clear-recent
                :label "Clear recent"
                :icon "delete"
                :action [::document.e/clear-recent]}]))))

(defn file-menu
  []
  {:id :file
   :label "File"
   :type :root
   :items [{:id :new-file
            :label "New"
            :icon "file"
            :action [::document.e/new]}
           {:id :divider-1
            :type :separator}
           {:id :open-file
            :label "Open…"
            :icon "folder"
            :action [::document.e/open nil]}
           {:id :recent
            :label "Recent"
            :type :sub-menu
            :disabled (not @(rf/subscribe [::document.s/some-recent]))
            :items (recent-submenu)}
           {:id :divider-2
            :type :separator}
           {:id :save
            :label "Save"
            :icon "save"
            :action [::document.e/save]
            :disabled (or (not @(rf/subscribe [::document.s/some]))
                          @(rf/subscribe [::document.s/active-saved]))}
           {:id :save-as
            :label "Save as…"
            :icon "save-as"
            :action [::document.e/save-as]
            :disabled (not @(rf/subscribe [::document.s/some]))}
           {:id :download
            :icon "download"
            :label "Download"
            :disabled (not @(rf/subscribe [::document.s/some]))
            :action [::document.e/download]}
           {:id :divider-3
            :type :separator}
           {:id :export-svg
            :label "Export as SVG"
            :icon "export"
            :disabled (not @(rf/subscribe [::document.s/some]))
            :action [::element.e/export-svg]}
           {:id :divider-4
            :type :separator}
           {:id :print
            :label "Print"
            :icon "printer"
            :disabled (not @(rf/subscribe [::document.s/some]))
            :action [::element.e/print]}
           {:id :divider-5
            :type :separator}
           {:id :close
            :label "Close"
            :icon "window-close"
            :disabled (not @(rf/subscribe [::document.s/some]))
            :action [::document.e/close-active]}
           {:id :exit
            :label "Exit"
            :icon "exit"
            :action [::window.e/close]}]})

(defn edit-menu
  []
  {:id :edit
   :label "Edit"
   :type :root
   :disabled (not @(rf/subscribe [::document.s/some]))
   :items [{:id :undo
            :label "Undo"
            :icon "undo"
            :disabled (not @(rf/subscribe [::history.s/undos?]))
            :action [::history.e/undo]}
           {:id :redo
            :label "Redo"
            :icon "redo"
            :disabled (not @(rf/subscribe [::history.s/redos?]))
            :action [::history.e/redo]}
           {:id :divider-1
            :type :separator}
           {:id :cut
            :label "Cut"
            :icon "cut"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/cut]}
           {:id :copy
            :icon "copy"
            :label "Copy"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/copy]}
           {:id :paste
            :label "Paste"
            :icon "paste"
            :action [::element.e/paste]}
           {:id :paste-in-place
            :icon "paste"
            :label "Paste in place"
            :action [::element.e/paste-in-place]}
           {:id :paste-styles
            :icon "paste"
            :label "Paste styles"
            :action [::element.e/paste-styles]}
           {:id :divider-2
            :type :separator}
           {:id :duplicate
            :icon "copy"
            :label "Duplicate"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/duplicate]}
           {:id :divider-3
            :type :separator}
           {:id :select-all
            :icon "select-all"
            :label "Select all"
            :action [::element.e/select-all]}
           {:id :deselect-all
            :icon "deselect-all"
            :label "Deselect all"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/deselect-all]}
           {:id :invert-selection
            :label "Invert selection"
            :icon "invert-selection"
            :action [::element.e/invert-selection]}
           {:id :select-same-tags
            :icon "select-same"
            :label "Select same tags"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/select-same-tags]}
           {:id :divider-4
            :type :separator}
           {:id :delete
            :icon "delete"
            :label "Delete"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/delete]}]})

(defn align-submenu
  []
  [{:id :align-left
    :label "Left"
    :icon "objects-align-left"
    :disabled @(rf/subscribe [::element.s/every-top-level])
    :action [::element.e/align :left]}
   {:id :align-center-horizontally
    :label "Center horizontally"
    :icon "objects-align-center-horizontal"
    :action [::element.e/align :center-horizontal]}
   {:id :align-right
    :label "Right"
    :icon "objects-align-right"
    :action [::element.e/align :right]}
   {:id :divider-1
    :type :separator}
   {:id :align-top
    :label "Top"
    :icon "objects-align-top"
    :action [::element.e/align :top]}
   {:id :align-center-vertically
    :label "Center vertically"
    :icon "objects-align-center-vertical"
    :action [::element.e/align :center-vertical]}
   {:id :align-bottom
    :label "Bottom"
    :icon "objects-align-bottom"
    :action [::element.e/align :bottom]}])

(defn boolean-submenu
  []
  [{:id :exclude
    :label "Exclude"
    :icon "exclude"
    :action [::element.e/boolean-operation :exclude]}
   {:id :unite
    :label "Unite"
    :icon "unite"
    :action [::element.e/boolean-operation :unite]}
   {:id :intersect
    :label "Intersect"
    :icon "intersect"
    :action [::element.e/boolean-operation :intersect]}
   {:id :subtract
    :label "Subtract"
    :icon "subtract"
    :action [::element.e/boolean-operation :subtract]}
   {:id :divide
    :label "Divide"
    :icon "divide"
    :action [::element.e/boolean-operation :divide]}])

(defn animate-submenu
  []
  [{:id :animate
    :label "Animate"
    :icon "animation"
    :action [::element.e/animate :animate {}]}
   {:id :animate-transform
    :label "Animate Transform"
    :icon "animation"
    :action [::element.e/animate :animateTransform {}]}
   {:id :animate-motion
    :icon "animation"
    :label "Animate Motion"
    :action [::element.e/animate :animateMotion {}]}])

(defn path-submenu
  []
  [{:id :simplify
    :label "Simplify"
    :icon "bezier-curve"
    :action [::element.e/manipulate-path :simplify]}
   {:id :smooth
    :label "Smooth"
    :icon "bezier-curve"
    :action [::element.e/manipulate-path :smooth]}
   {:id :flatten
    :label "Flatten"
    :icon "bezier-curve"
    :action [::element.e/manipulate-path :flatten]}
   {:id :reverse
    :label "Reverse"
    :icon "bezier-curve"
    :action [::element.e/manipulate-path :reverse]}])

(defn image-submenu
  []
  [{:id :trace
    :label "Trace"
    :icon "image"
    :action [::element.e/trace]}])

(defn object-menu
  []
  {:id :object
   :label "Object"
   :type :root
   :disabled (not @(rf/subscribe [::document.s/some]))
   :items [{:id :to-path
            :label "Object to path"
            :icon "bezier-curve"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/->path]}
           {:id :stroke-to-path
            :label "Stroke to path"
            :icon "bezier-curve"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/stroke->path]}
           {:id :divider-1
            :type :separator}
           {:id :group
            :label "Group"
            :icon "group"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/group]}
           {:id :ungroup
            :label "Ungroup"
            :icon "ungroup"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/ungroup]}
           {:id :divider-2
            :type :separator}
           {:id :lock
            :label "Lock"
            :icon "lock"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/lock]}
           {:id :unlock
            :label "Unlock"
            :icon "unlock"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/unlock]}
           {:id :divider-3
            :type :separator}
           {:id :path
            :label "Align"
            :type :sub-menu
            :disabled @(rf/subscribe [::element.s/every-top-level])
            :items (align-submenu)}
           {:id :boolean
            :label "Animate"
            :type :sub-menu
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :items (animate-submenu)}
           {:id :boolean
            :label "Boolean operation"
            :type :sub-menu
            :disabled (not @(rf/subscribe [::element.s/multiple-selected]))
            :items (boolean-submenu)}
           {:id :divider-4
            :type :separator}
           {:id :raise
            :label "Raise"
            :icon "bring-forward"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/raise]}
           {:id :lower
            :label "Lower"
            :icon "send-backward"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/lower]}
           {:id :raise-to-top
            :label "Raise to top"
            :icon "bring-front"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/raise-to-top]}
           {:id :lower-to-bottom
            :label "Lower to bottom"
            :icon "send-back"
            :disabled (not @(rf/subscribe [::element.s/some-selected]))
            :action [::element.e/lower-to-bottom]}
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
    :action [::frame.e/zoom-in]}
   {:id :zoom-out
    :label "Out"
    :icon "zoom-out"
    :action [::frame.e/zoom-out]}
   {:id :divider-1
    :type :separator}
   {:label "Set to 50%"
    :id "50"
    :icon "magnifier"
    :action [::frame.e/set-zoom 0.5]}
   {:label "Set to 100%"
    :id "100"
    :icon "magnifier"
    :action [::frame.e/set-zoom 1]}
   {:label "Set to 200%"
    :id "200"
    :icon "magnifier"
    :action [::frame.e/set-zoom 2]}
   {:id :divider-2
    :type :separator}
   {:label "Focus selected"
    :id "focus-selected"
    :icon "focus"
    :action [::frame.e/focus-selection :original]}
   {:label "Fit selected"
    :id "fit-selected"
    :icon "focus"
    :action [::frame.e/focus-selection :fit]}
   {:label "Fill selected"
    :id "fill-selected"
    :icon "focus"
    :action [::frame.e/focus-selection :fill]}])

(defn a11y-submenu
  []
  (mapv (fn [{:keys [id]}]
          {:id id
           :label (name id)
           :type :checkbox
           :icon "a11y"
           :checked @(rf/subscribe [::document.s/filter-active id])
           :action [::document.e/toggle-filter id]}) filters/accessibility))

(defn panel-submenu
  []
  [{:id :toggle-tree
    :type :checkbox
    :icon "tree"
    :label "Element tree"
    :checked @(rf/subscribe [::app.s/panel-visible :tree])
    :action [::app.e/toggle-panel :tree]}
   {:id :toggle-props
    :type :checkbox
    :icon "properties"
    :label "Properties"
    :checked @(rf/subscribe [::app.s/panel-visible :properties])
    :action [::app.e/toggle-panel :properties]}
   {:id :toggle-xml
    :label "XML view"
    :type :checkbox
    :icon "code"
    :checked @(rf/subscribe [::app.s/panel-visible :xml])
    :action [::app.e/toggle-panel :xml]}
   {:id :toggle-history
    :label "History tree"
    :icon "history"
    :type :checkbox
    :checked @(rf/subscribe [::app.s/panel-visible :history])
    :action [::app.e/toggle-panel :history]}
   {:id :toggle-command-history
    :type :checkbox
    :label "Shell history"
    :icon "shell"
    :checked @(rf/subscribe [::app.s/panel-visible :repl-history])
    :action [::app.e/toggle-panel :repl-history]}
   {:id :toggle-timeline-panel
    :type :checkbox
    :label "Timeline editor"
    :icon "timeline"
    :checked @(rf/subscribe [::app.s/panel-visible :timeline])
    :action [::app.e/toggle-panel :timeline]}
   {:id :divider-2
    :type :separator}])

(defn view-menu
  []
  {:id :view
   :label "View"
   :type :root
   :disabled (not @(rf/subscribe [::document.s/some]))
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
            :checked @(rf/subscribe [::app.s/grid])
            :action [::app.e/toggle-grid]}
           {:id :toggle-ruler
            :type :checkbox
            :label "Rulers"
            :icon "ruler-combined"
            :checked @(rf/subscribe [::ruler.s/visible])
            :action [::ruler.e/toggle-visible]}
           {:id :toggle-debug-info
            :type :checkbox
            :label "Debug info"
            :icon "bug"
            :checked @(rf/subscribe [::app.s/debug-info])
            :action [::app.e/toggle-debug-info]}
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
            :checked @(rf/subscribe [::window.s/fullscreen])
            :action [::window.e/toggle-fullscreen]}]})

(defn help-menu
  []
  {:id :help
   :label "Help"
   :type :root
   :items [{:id :cmdk
            :label "Command panel"
            :icon "command"
            :action [::dialog.e/cmdk]}
           {:id :divider-1
            :type :separator}
           {:id :website
            :label "Website"
            :icon "earth"
            :action [::window.e/open-remote-url
                     "https://repath.studio/"]}
           {:id :source-code
            :label "Source Code"
            :icon "commit"
            :action [::window.e/open-remote-url
                     "https://github.com/repath-project/repath-studio"]}
           {:id :license
            :label "License"
            :icon "lgpl"
            :action [::window.e/open-remote-url
                     "https://github.com/repath-project/repath-studio/blob/main/LICENSE"]}
           {:id :changelog
            :icon "list"
            :label "Changelog"
            :action [::window.e/open-remote-url
                     "https://repath.studio/roadmap/changelog/"]}
           {:id :divider-2
            :type :separator}
           {:id :submit-issue
            :icon "warning"
            :label "Submit an issue"
            :action [::window.e/open-remote-url
                     "https://github.com/repath-project/repath-studio/issues/new/choose"]}
           {:id :divider-3
            :type :separator}
           {:id :about
            :icon "info"
            :label "About"
            :action [::dialog.e/about]}]})

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
    :on-select #(rf/dispatch [::e/select-item action])
    :disabled disabled}
   label
   [:div.right-slot
    [ui/shortcuts action]]])

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
          :on-key-down #(.stopPropagation %) ; FIXME: Esc global action also triggered.
          :on-value-change #(rf/dispatch [::app.e/set-backdrop (boolean (seq %))])}]
        (map menu-item (root-menu))))
