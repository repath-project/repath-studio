(ns renderer.tool.db
  (:require [renderer.tool.hierarchy :as hierarchy]))

(defn tool?
  [tool]
  (isa? tool ::hierarchy/tool))

(def Tool
  [:fn {:error/fn (fn [{:keys [value]} _] (str value " is not a supported tool"))}
   tool?])

(def State [:enum :idle :translate :clone :scale :select :create :edit])

(def Cursor
  [:enum
   "auto"
   "default"
   "none"
   "context-menu"
   "help"
   "pointer"
   "progress"
   "wait"
   "cell"
   "crosshair"
   "text"
   "vertical-text"
   "alias"
   "copy"
   "move"
   "no-drop"
   "not-allowed"
   "grab"
   "grabbing"
   "e-resize"
   "n-resize"
   "ne-resize"
   "nw-resize"
   "s-resize"
   "se-resize"
   "sw-resize"
   "w-resize"
   "ew-resize"
   "ns-resize"
   "nesw-resize"
   "nwse-resize"
   "col-resize"
   "row-resize"
   "all-scroll"
   "zoom-in"
   "zoom-out"])
