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
   [renderer.error.events :as-alias error.events]
   [renderer.error.subs :as-alias error.subs]
   [renderer.events :as-alias events]
   [renderer.frame.events :as-alias frame.events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]
   [renderer.menubar.events :as-alias menubar.events]
   [renderer.menubar.filters :as filters]
   [renderer.ruler.events :as-alias ruler.events]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.utils.i18n :as utils.i18n :refer [t]]
   [renderer.views :as views]
   [renderer.window.events :as-alias window.events]
   [renderer.window.subs :as-alias window.subs]))

(defn recent-submenu
  []
  (let [recent @(rf/subscribe [::document.subs/recent])
        recent-items (mapv (fn [path]
                             {:id (keyword path)
                              :label path
                              :icon "folder"
                              :action [::document.events/open path]}) recent)]
    (cond-> recent-items
      (seq recent-items)
      (concat [{:id :divider-1
                :type :separator}
               {:id :clear-recent
                :label (t [::recent-clear "Clear recent"])
                :icon "delete"
                :action [::document.events/clear-recent]}]))))

(defn export-submenu
  []
  [{:id :export-svg
    :label "SVG"
    :icon "export"
    :disabled (not @(rf/subscribe [::document.subs/entities?]))
    :action [::document.events/export "image/svg+xml"]}
   {:id :divider-1
    :type :separator}
   {:id :export-png
    :label "PNG"
    :icon "export"
    :disabled (not @(rf/subscribe [::document.subs/entities?]))
    :action [::document.events/export "image/png"]}
   {:id :export-jpg
    :label "JPG"
    :icon "export"
    :disabled (not @(rf/subscribe [::document.subs/entities?]))
    :action [::document.events/export "image/jpeg"]}
   {:id :export-webp
    :label "WEBP"
    :icon "export"
    :disabled (not @(rf/subscribe [::document.subs/entities?]))
    :action [::document.events/export "image/webp"]}
   {:id :export-gif
    :label "GIF"
    :icon "export"
    :disabled (not @(rf/subscribe [::document.subs/entities?]))
    :action [::document.events/export "image/gif"]}])

(defn file-menu
  []
  {:id :file
   :label (t [::file "File"])
   :type :root
   :items [{:id :new-file
            :label (t [::new "New"])
            :icon "file"
            :action [::document.events/new]}
           {:id :divider-1
            :type :separator}
           {:id :open-file
            :label (t [::open "Open..."])
            :icon "folder"
            :action [::document.events/open nil]}
           {:id :recent
            :label (t [::recent "Recent"])
            :type :sub-menu
            :disabled (not @(rf/subscribe [::document.subs/recent?]))
            :items (recent-submenu)}
           {:id :divider-2
            :type :separator}
           (if @(rf/subscribe [::app.subs/feature-available? :file-system])
             {:id :save
              :label (t [::save "Save"])
              :icon "save"
              :action [::document.events/save]
              :disabled (or (not @(rf/subscribe [::document.subs/entities?]))
                            @(rf/subscribe [::document.subs/active-saved?]))}
             {:id :download
              :icon "download"
              :label (t [::download "Download"])
              :disabled (not @(rf/subscribe [::document.subs/entities?]))
              :action [::document.events/download]})
           {:id :save-as
            :label (t [::save-as "Save as..."])
            :icon "save-as"
            :action [::document.events/save-as]
            :disabled (not @(rf/subscribe [::document.subs/entities?]))}
           {:id :export
            :label (t [::export-as "Export as"])
            :type :sub-menu
            :disabled (not @(rf/subscribe [::document.subs/entities?]))
            :items (export-submenu)}
           {:id :divider-3
            :type :separator}
           {:id :print
            :label (t [::print "Print"])
            :icon "printer"
            :disabled (not @(rf/subscribe [::document.subs/entities?]))
            :action [::document.events/print]}
           {:id :divider-4
            :type :separator}
           {:id :close
            :label (t [::close "Close"])
            :icon "window-close"
            :disabled (not @(rf/subscribe [::document.subs/entities?]))
            :action [::document.events/close-active]}
           {:id :exit
            :label (t [::exit "Exit"])
            :icon "exit"
            :action [::window.events/close]}]})

(defn edit-menu
  []
  {:id :edit
   :label (t [::edit "Edit"])
   :type :root
   :disabled (not @(rf/subscribe [::document.subs/entities?]))
   :items [{:id :undo
            :label (t [::undo "Undo"])
            :icon "undo"
            :disabled (not @(rf/subscribe [::history.subs/undos?]))
            :action [::history.events/undo]}
           {:id :redo
            :label (t [::redo "Redo"])
            :icon "redo"
            :disabled (not @(rf/subscribe [::history.subs/redos?]))
            :action [::history.events/redo]}
           {:id :divider-1
            :type :separator}
           {:id :cut
            :label (t [::cut "Cut"])
            :icon "cut"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/cut]}
           {:id :copy
            :icon "copy"
            :label (t [::copy "Copy"])
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/copy]}
           {:id :paste
            :label (t [::paste "Paste"])
            :icon "paste"
            :action [::element.events/paste]}
           {:id :paste-in-place
            :icon "paste"
            :label (t [::paste-in-place "Paste in place"])
            :action [::element.events/paste-in-place]}
           {:id :paste-styles
            :icon "paste"
            :label (t [::paste-styles "Paste styles"])
            :action [::element.events/paste-styles]}
           {:id :divider-2
            :type :separator}
           {:id :duplicate
            :icon "copy"
            :label (t [::duplicate "Duplicate"])
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/duplicate]}
           {:id :divider-3
            :type :separator}
           {:id :select-all
            :icon "select-all"
            :label (t [::select-all "Select all"])
            :action [::element.events/select-all]}
           {:id :deselect-all
            :icon "deselect-all"
            :label (t [::deselect-all "Deselect all"])
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/deselect-all]}
           {:id :invert-selection
            :label (t [::invert-selection "Invert selection"])
            :icon "invert-selection"
            :action [::element.events/invert-selection]}
           {:id :select-same-tags
            :icon "select-same"
            :label (t [::select-same-tags "Select same tags"])
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/select-same-tags]}
           {:id :divider-4
            :type :separator}
           {:id :delete
            :icon "delete"
            :label (t [::delete "Delete"])
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/delete]}]})

(defn align-submenu
  []
  [{:id :align-left
    :label (t [::align-left "Left"])
    :icon "objects-align-left"
    :disabled @(rf/subscribe [::element.subs/every-top-level])
    :action [::element.events/align :left]}
   {:id :align-center-horizontally
    :label (t [::align-center-horizontally "Center horizontally"])
    :icon "objects-align-center-horizontal"
    :action [::element.events/align :center-horizontal]}
   {:id :align-right
    :label (t [::align-right "Right"])
    :icon "objects-align-right"
    :action [::element.events/align :right]}
   {:id :divider-1
    :type :separator}
   {:id :align-top
    :label (t [::align-top "Top"])
    :icon "objects-align-top"
    :action [::element.events/align :top]}
   {:id :align-center-vertically
    :label (t [::align-center-vertically "Center vertically"])
    :icon "objects-align-center-vertical"
    :action [::element.events/align :center-vertical]}
   {:id :align-bottom
    :label (t [::align-bottom "Bottom"])
    :icon "objects-align-bottom"
    :action [::element.events/align :bottom]}])

(defn boolean-submenu
  []
  [{:id :exclude
    :label (t [::boolean-exclude "Exclude"])
    :icon "exclude"
    :action [::element.events/boolean-operation :exclude]}
   {:id :unite
    :label (t [::boolean-unite "Unite"])
    :icon "unite"
    :action [::element.events/boolean-operation :unite]}
   {:id :intersect
    :label (t [::boolean-intersect "Intersect"])
    :icon "intersect"
    :action [::element.events/boolean-operation :intersect]}
   {:id :subtract
    :label (t [::boolean-subtract "Subtract"])
    :icon "subtract"
    :action [::element.events/boolean-operation :subtract]}
   {:id :divide
    :label (t [::boolean-divide "Divide"])
    :icon "divide"
    :action [::element.events/boolean-operation :divide]}])

(defn animate-submenu
  []
  [{:id :animate
    :label (t [::animate "Animate"])
    :icon "animation"
    :action [::element.events/animate :animate {}]}
   {:id :animate-transform
    :label (t [::animate-transform "Animate Transform"])
    :icon "animation"
    :action [::element.events/animate :animateTransform {}]}
   {:id :animate-motion
    :icon "animation"
    :label (t [::animate-motion "Animate Motion"])
    :action [::element.events/animate :animateMotion {}]}])

(defn path-submenu
  []
  [{:id :simplify
    :label (t [::path-simplify "Simplify"])
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :simplify]}
   {:id :smooth
    :label (t [::path-smooth "Smooth"])
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :smooth]}
   {:id :flatten
    :label (t [::path-flatten "Flatten"])
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :flatten]}
   {:id :reverse
    :label (t [::path-reverse "Reverse"])
    :icon "bezier-curve"
    :action [::element.events/manipulate-path :reverse]}])

(defn image-submenu
  []
  [{:id :trace
    :label (t [::image-trace "Trace"])
    :icon "image"
    :action [::element.events/trace]}])

(defn object-menu
  []
  {:id :object
   :label (t [::object "Object"])
   :type :root
   :disabled (not @(rf/subscribe [::document.subs/entities?]))
   :items [{:id :to-path
            :label (t [::object-to-path "Object to path"])
            :icon "bezier-curve"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/->path]}
           {:id :stroke-to-path
            :label (t [::stroke-to-path "Stroke to path"])
            :icon "bezier-curve"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/stroke->path]}
           {:id :divider-1
            :type :separator}
           {:id :group
            :label (t [::group "Group"])
            :icon "group"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/group]}
           {:id :ungroup
            :label (t [::ungroup "Ungroup"])
            :icon "ungroup"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/ungroup]}
           {:id :divider-2
            :type :separator}
           {:id :lock
            :label (t [::lock "Lock"])
            :icon "lock"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/lock]}
           {:id :unlock
            :label (t [::unlock "Unlock"])
            :icon "unlock"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/unlock]}
           {:id :divider-3
            :type :separator}
           {:id :path
            :label (t [::align "Align"])
            :type :sub-menu
            :disabled @(rf/subscribe [::element.subs/every-top-level])
            :items (align-submenu)}
           {:id :boolean
            :label (t [::animate "Animate"])
            :type :sub-menu
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :items (animate-submenu)}
           {:id :boolean
            :label (t [::boolean-operation "Boolean operation"])
            :type :sub-menu
            :disabled (not @(rf/subscribe [::element.subs/multiple-selected?]))
            :items (boolean-submenu)}
           {:id :divider-4
            :type :separator}
           {:id :raise
            :label (t [::raise "Raise"])
            :icon "bring-forward"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/raise]}
           {:id :lower
            :label (t [::lower "Lower"])
            :icon "send-backward"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/lower]}
           {:id :raise-to-top
            :label (t [::raise-to-top "Raise to top"])
            :icon "bring-front"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/raise-to-top]}
           {:id :lower-to-bottom
            :label (t [::lower-to-bottom "Lower to bottom"])
            :icon "send-back"
            :disabled (not @(rf/subscribe [::element.subs/some-selected?]))
            :action [::element.events/lower-to-bottom]}
           {:id :divider-5
            :type :separator}
           {:id :image
            :type :sub-menu
            :label (t [::image "Image"])
            :items (image-submenu)}
           {:id :path
            :label (t [::path "Path"])
            :type :sub-menu
            :items (path-submenu)}]})

(defn zoom-submenu
  []
  [{:id :zoom-in
    :label (t [::zoom-in "In"])
    :icon "zoom-in"
    :action [::frame.events/zoom-in]}
   {:id :zoom-out
    :label (t [::zoom-out "Out"])
    :icon "zoom-out"
    :action [::frame.events/zoom-out]}
   {:id :divider-1
    :type :separator}
   {:label (t [::zoom-set-50 "Set to 50%"])
    :id "50"
    :icon "magnifier"
    :action [::frame.events/set-zoom 0.5]}
   {:label (t [::zoom-set-100 "Set to 100%"])
    :id "100"
    :icon "magnifier"
    :action [::frame.events/set-zoom 1]}
   {:label (t [::zoom-set-200 "Set to 200%"])
    :id "200"
    :icon "magnifier"
    :action [::frame.events/set-zoom 2]}
   {:id :divider-2
    :type :separator}
   {:label (t [::zoom-focus-selected "Focus selected"])
    :id "focus-selected"
    :icon "focus"
    :action [::frame.events/focus-selection :original]}
   {:label (t [::zoom-fit-selected "Fit selected"])
    :id "fit-selected"
    :icon "focus"
    :action [::frame.events/focus-selection :fit]}
   {:label (t [::zoom-fill-selected "Fill selected"])
    :id "fill-selected"
    :icon "focus"
    :action [::frame.events/focus-selection :fill]}])

(defn a11y-submenu
  []
  (->> (filters/accessibility)
       (mapv (fn [{:keys [id label]}]
               {:id id
                :label label
                :type :checkbox
                :icon "a11y"
                :checked @(rf/subscribe [::document.subs/filter-active id])
                :action [::document.events/toggle-filter id]}))))

(defn languages-submenu
  []
  (->> utils.i18n/languages
       (mapv (fn [[k v]]
               {:id k
                :abbr (:abbr v)
                :label (:native-name v)
                :type :checkbox
                :icon "language"
                :action [::app.events/set-lang k]
                :checked (= @(rf/subscribe [::app.subs/lang]) k)}))
       (into [{:id "system"
               :label "System"
               :type :checkbox
               :icon "language"
               :action [::app.events/set-lang "system"]
               :checked (= @(rf/subscribe [::app.subs/lang]) "system")}])))

(defn panel-submenu
  []
  [{:id :toggle-tree
    :type :checkbox
    :icon "tree"
    :label (t [::panel-element-tree "Element tree"])
    :checked @(rf/subscribe [::app.subs/panel-visible? :tree])
    :action [::app.events/toggle-panel :tree]}
   {:id :toggle-props
    :type :checkbox
    :icon "properties"
    :label (t [::panel-properties "Properties"])
    :checked @(rf/subscribe [::app.subs/panel-visible? :properties])
    :action [::app.events/toggle-panel :properties]}
   {:id :toggle-xml
    :label (t [::panel-xml-view "XML view"])
    :type :checkbox
    :icon "code"
    :checked @(rf/subscribe [::app.subs/panel-visible? :xml])
    :action [::app.events/toggle-panel :xml]}
   {:id :toggle-history
    :label (t [::panel-history-tree "History tree"])
    :icon "history"
    :type :checkbox
    :checked @(rf/subscribe [::app.subs/panel-visible? :history])
    :action [::app.events/toggle-panel :history]}
   {:id :toggle-command-history
    :type :checkbox
    :label (t [::panel-shell-history "Shell history"])
    :icon "shell"
    :checked @(rf/subscribe [::app.subs/panel-visible? :repl-history])
    :action [::app.events/toggle-panel :repl-history]}
   {:id :toggle-timeline-panel
    :type :checkbox
    :label (t [::panel-timeline-editor "Timeline editor"])
    :icon "timeline"
    :checked @(rf/subscribe [::app.subs/panel-visible? :timeline])
    :action [::app.events/toggle-panel :timeline]}
   {:id :divider-2
    :type :separator}])

(defn view-menu
  []
  {:id :view
   :label (t [::view "View"])
   :type :root
   :items [{:id :zoom
            :label (t [::zoom "Zoom"])
            :type :sub-menu
            :disabled (not @(rf/subscribe [::document.subs/entities?]))
            :items (zoom-submenu)}
           {:id :a11y
            :label (t [::accessibility-filter "Accessibility filter"])
            :type :sub-menu
            :disabled (not @(rf/subscribe [::document.subs/entities?]))
            :items (a11y-submenu)}
           {:id :lang
            :label (t [::language "Language"])
            :type :sub-menu
            :items (languages-submenu)}
           {:id :divider-1
            :type :separator}
           {:id :toggle-grid
            :type :checkbox
            :label (t [::grid "Grid"])
            :icon "grid"
            :checked @(rf/subscribe [::app.subs/grid])
            :action [::app.events/toggle-grid]}
           {:id :toggle-ruler
            :type :checkbox
            :label (t [::rulers "Rulers"])
            :icon "ruler-combined"
            :checked @(rf/subscribe [::ruler.subs/visible?])
            :action [::ruler.events/toggle-visible]}
           {:id :help-bar
            :type :checkbox
            :label (t [::help-bar "Help bar"])
            :icon "info"
            :checked @(rf/subscribe [::app.subs/help-bar])
            :action [::app.events/toggle-help-bar]}
           {:id :toggle-debug-info
            :type :checkbox
            :label (t [::debug-info "Debug info"])
            :icon "bug"
            :checked @(rf/subscribe [::app.subs/debug-info])
            :action [::app.events/toggle-debug-info]}
           {:id :divider-2
            :type :separator}
           {:id :panel
            :label (t [::panel "Panel"])
            :type :sub-menu
            :items (panel-submenu)}
           {:id :divider-3
            :type :separator}
           {:id :toggle-fullscreen
            :label (t [::fullscreen "Fullscreen"])
            :icon "arrow-minimize"
            :type :checkbox
            :checked @(rf/subscribe [::window.subs/fullscreen?])
            :action [::window.events/toggle-fullscreen]}]})

(defn help-menu
  []
  {:id :help
   :label (t [::help "Help"])
   :type :root
   :items [{:id :cmdk
            :label (t [::command-panel "Command panel"])
            :icon "command"
            :action [::dialog.events/show-cmdk]}
           {:id :divider-1
            :type :separator}
           {:id :website
            :label (t [::website "Website"])
            :icon "earth"
            :action [::events/open-remote-url
                     "https://repath.studio/"]}
           {:id :source-code
            :label (t [::source-code "Source Code"])
            :icon "commit"
            :action [::events/open-remote-url
                     "https://github.com/repath-project/repath-studio"]}
           {:id :license
            :label (t [::license "License"])
            :icon "lgpl"
            :action [::events/open-remote-url
                     "https://github.com/repath-project/repath-studio/blob/main/LICENSE"]}
           {:id :changelog
            :icon "list"
            :label (t [::changelog "Changelog"])
            :action [::events/open-remote-url
                     "https://repath.studio/roadmap/changelog/"]}
           {:id :privacy-policy
            :icon "list"
            :label (t [::privacy-policy "Privacy Policy"])
            :action [::events/open-remote-url
                     "https://repath.studio/policies/privacy/"]}
           {:id :divider-2
            :type :separator}
           {:id :submit-issue
            :icon "warning"
            :label (t [::submit-an-issue "Submit an issue"])
            :action [::events/open-remote-url
                     "https://github.com/repath-project/repath-studio/issues/new/choose"]}
           {:id :report-errors
            :icon "bug"
            :type :checkbox
            :label (t [::report-errors "Report errors automatically"])
            :checked @(rf/subscribe [::error.subs/reporting?])
            :action [::error.events/toggle-reporting]}
           {:id :divider-3
            :type :separator}
           {:id :about
            :icon "info"
            :label (t [::about "About"])
            :action [::dialog.events/show-about]}]})

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
    [views/icon "checkmark"]]
   [:div label]
   [views/shortcuts action]])

(defmethod menu-item :sub-menu
  [{:keys [label items disabled]}]
  [:> Menubar/Sub
   [:> Menubar/SubTrigger
    {:class "sub-menu-item menu-item"
     :disabled disabled}
    [:div label]
    [:div.sub-menu-chevron
     {:class "rtl:scale-x-[-1]"}
     [views/icon "chevron-right"]]]
   [:> Menubar/Portal
    (into [:> Menubar/SubContent
           {:class "menu-content"
            :align "start"
            :loop true
            :on-escape-key-down #(.stopPropagation %)}]
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
            :loop true
            :on-escape-key-down #(.stopPropagation %)}]
          (map menu-item items))]])

(defmethod menu-item :default
  [{:keys [label action disabled]}]
  [:> Menubar/Item
   {:class "menu-item"
    :on-select #(rf/dispatch [::menubar.events/select-item action])
    :disabled disabled}
   [:div label]
   [views/shortcuts action]])

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
         {:class "flex"
          :on-key-down #(.stopPropagation %)
          :on-value-change #(rf/dispatch [::app.events/set-backdrop
                                          (-> (seq %)
                                              (boolean))])}]
        (map menu-item (submenus))))
