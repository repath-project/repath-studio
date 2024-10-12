(ns components.icon-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.ui :as ui]))

(def default-icons
  ["a11y"
   "animation"
   "arc"
   "arrow-minimize"
   "bezier-curve"
   "blob"
   "bring-forward"
   "bring-front"
   "brush"
   "bug"
   "checkmark"
   "chemical-element"
   "chevron-down"
   "chevron-left"
   "chevron-right"
   "chevron-up"
   "circle-tool"
   "circle"
   "code"
   "command"
   "commit"
   "copy"
   "cut"
   "dark"
   "degrees"
   "delete"
   "deselect-all"
   "distribute-spacing-horizontal"
   "distribute-spacing-vertical"
   "divide"
   "dot"
   "download"
   "earth"
   "edit"
   "ellipse-tool"
   "ellipse"
   "ellipsis-h"
   "ellipsis-v"
   "exclude"
   "exit"
   "export"
   "eye-closed"
   "eye-dropper"
   "eye"
   "file"
   "fill"
   "flip-horizontal"
   "flip-vertical"
   "focus"
   "folder-plus"
   "folder"
   "go-to-end"
   "go-to-start"
   "grid"
   "group"
   "hand"
   "history"
   "image"
   "import"
   "info"
   "intersect"
   "invert-selection"
   "lgpl"
   "light"
   "line-tool"
   "linecap-butt"
   "linecap-round"
   "linecap-square"
   "line"
   "list"
   "lock"
   "magnet"
   "magnifier"
   "minus"
   "objects-align-bottom"
   "objects-align-center-horizontal"
   "objects-align-center-vertical"
   "objects-align-left"
   "objects-align-right"
   "objects-align-top"
   "page-plus"
   "page"
   "paste"
   "pause"
   "pencil"
   "play"
   "plus"
   "pointer-tool"
   "pointer"
   "polygon-tool"
   "polygon"
   "polyline"
   "printer"
   "properties"
   "rectangle-tool"
   "rectangle"
   "redo"
   "refresh"
   "rotate-clockwise"
   "rotate-counterclockwise"
   "ruler-combined"
   "ruler-triangle"
   "save-as"
   "save"
   "select-all"
   "select-same"
   "send-back"
   "send-backward"
   "shell"
   "spinner"
   "square-minus"
   "square+plus"
   "stop"
   "subtract"
   "svg"
   "swap-horizontal"
   "swap-vertical"
   "system"
   "text"
   "timeline"
   "times"
   "tree"
   "triangle"
   "undo"
   "ungroup"
   "unite"
   "unlock"
   "warning"
   "window-close"
   "window-maximize"
   "window-minimize"
   "window-restore"
   "zoom-in"
   "zoom-out"])

(defscene default
  :title "Default icons"
  [ui/scroll-area
   [:div.flex.gap-2.p-3
    (for [icon-name default-icons]
      ^{:key icon-name}
      [:div {:title icon-name}
       [ui/icon icon-name]])]])

(def branded-icons
  ["android_head"
   "chrome"
   "edge"
   "firefox"
   "ie"
   "oculus"
   "opera"
   "safari"
   "samsunginternet_android"
   "webview_android"])

(defscene branded
  :title "Branded icons"
  [:div.flex.gap-2.p-3
   (for [icon-name branded-icons]
     ^{:key icon-name}
     [:div {:title icon-name}
      [ui/icon icon-name]])])
