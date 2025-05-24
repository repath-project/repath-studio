(ns renderer.event.db
  (:require
   [renderer.element.db :refer [Element]]
   [renderer.tool.db :refer [Handle]]
   [renderer.utils.math :refer [Vec2]]))

(def PointerButton [:enum :left :middle :right :back :forward])

(def KeyboardEvent
  [:map {:closed true}
   [:target any?]
   [:code string?]
   [:key-code number?]
   [:key string?]
   [:alt-key boolean?]
   [:ctrl-key boolean?]
   [:meta-key boolean?]
   [:shift-key boolean?]
   [:type [:enum "keydown" "keypress" "keyup"]]])

(def PointerEvent
  [:map {:closed true}
   [:element [:maybe [:or Element Handle]]]
   [:target any?]
   [:pointer-pos [:maybe Vec2]]
   [:pressure [:maybe number?]]
   [:pointer-type [:enum "mouse" "pen" "touch"]]
   [:pointer-id number?]
   [:timestamp number?]
   [:primary boolean?]
   [:button [:maybe PointerButton]]
   [:alt-key boolean?]
   [:ctrl-key boolean?]
   [:meta-key boolean?]
   [:shift-key boolean?]
   [:type [:enum
           "pointerover"
           "pointerenter"
           "pointerdown"
           "pointermove"
           "pointerrawupdate"
           "pointerup"
           "pointercancel"
           "pointerout"
           "pointerleave"
           "gotpointercapture"
           "lostpointercapture"]]])

(def WheelEvent
  [:map {:closed true}
   [:target any?]
   [:type [:= "wheel"]]
   [:pointer-pos [:maybe Vec2]]
   [:delta-x [:maybe number?]]
   [:delta-y [:maybe number?]]
   [:delta-z [:maybe number?]]
   [:alt-key boolean?]
   [:ctrl-key boolean?]
   [:meta-key boolean?]
   [:shift-key boolean?]])

;; https://developer.mozilla.org/en-US/docs/Web/API/DragEvent
(def DragEvent
  [:map {:closed true}
   [:target any?]
   [:pointer-pos [:maybe Vec2]]
   [:data-transfer any?]
   [:type [:enum
           "drag"
           "dragstart"
           "dragend"
           "dragenter"
           "dragleave"
           "dragover"
           "drop"]]])
