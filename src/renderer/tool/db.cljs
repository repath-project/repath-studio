(ns renderer.tool.db
  (:require [renderer.tool.hierarchy :as tool.hierarchy]))

(defn tool?
  [tool]
  (isa? tool ::tool.hierarchy/tool))

(def Tool
  [:fn {:error/fn (fn [{:keys [value]} _]
                    (str value " is not a supported tool"))}
   tool?])

(def State [:enum :idle :translate :clone :scale :select :create :edit :type])

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

(def HandleAction
  [:enum :translate :scale :edit])

(def Handle
  [:map {:closed true}
   [:id keyword?]
   [:label {:optional true} string?]
   [:action HandleAction]
   [:type [:= :handle]]
   [:cursor {:optional true} string?]
   [:x {:optional true} number?]
   [:y {:optional true} number?]
   [:size {:optional true} number?]
   [:stroke-width {:optional true} number?]
   [:element-id {:optional true} uuid?]])
