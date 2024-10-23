(ns renderer.utils.pointer
  (:require
   [clojure.core.matrix :as mat]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.document.db :refer [ZoomFactor]]
   [renderer.element.db :refer [Element]]
   [renderer.handle.db :refer [Handle]]
   [renderer.tool.events :as-alias tool.e]
   [renderer.utils.keyboard :refer [ModifierKey modifiers]]
   [renderer.utils.math :refer [Vec2D]]))

(def PointerButton [:enum :left :middle :right :back :forward])

(def PointerEvent
  [:map {:closed true}
   [:element [:maybe [:or Element Handle]]]
   [:target any?]
   [:type [:enum "pointerover" "pointerenter" "pointerdown" "pointermove" "pointerrawupdate" "pointerup" "pointercancel" "pointerout" "pointerleave" "gotpointercapture" "lostpointercapture"]]
   [:pointer-pos [:maybe Vec2D]]
   [:pressure [:maybe number?]]
   [:pointer-type [:enum "mouse" "pen" "touch"]]
   [:pointer-id number?]
   [:primary boolean?]
   [:button [:maybe PointerButton]]
   [:buttons [:maybe PointerButton]]
   [:modifiers [:set ModifierKey]]])

(m/=> ctrl? [:-> map? boolean?])
(defn ctrl?
  [e]
  (contains? (:modifiers e) :ctrl))

(m/=> shift? [:-> map? boolean?])
(defn shift?
  [e]
  (contains? (:modifiers e) :shift))

(m/=> alt? [:-> map? boolean?])
(defn alt?
  [e]
  (contains? (:modifiers e) :alt))

(m/=> adjust-position [:-> ZoomFactor Vec2D Vec2D Vec2D])
(defn adjust-position
  [zoom pan pointer-pos]
  (-> pointer-pos
      (mat/div zoom)
      (mat/add pan)))

(m/=> button->key [:-> [:enum -1 0 1 2 3 4] [:maybe PointerButton]])
(defn button->key
  "https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/button"
  [button]
  (get {0 :left
        1 :middle
        2 :right
        3 :back
        4 :forward} button))

(m/=> lock-direction [:-> Vec2D Vec2D])
(defn lock-direction
  "Locks pointer movement to the axis with the biggest offset"
  [[x y]]
  (if (> (abs x) (abs y))
    [x 0]
    [0 y]))

(defn event-handler!
  "Gathers pointer event props.
   https://developer.mozilla.org/en-US/docs/Web/API/PointerEvent

   Then dispathces the corresponding event.
   https://day8.github.io/re-frame/FAQs/Null-Dispatched-Events/

   Although the fps might drop because synced dispatch blocks the rendering,
   the end result appears to be more responsive because it's synced with the
   pointer movement."
  [^js/PointerEvent e el]
  (.stopPropagation e)

  (when (= (.-pointerType e) "touch")
    (.preventDefault e))

  (rf/dispatch-sync [::tool.e/pointer-event {:element el
                                             :target (.-target e)
                                             :type (.-type e)
                                             :pointer-pos [(.-pageX e) (.-pageY e)]
                                             :pressure (.-pressure e)
                                             :pointer-type (.-pointerType e)
                                             :pointer-id (.-pointerId e)
                                             :primary (.-isPrimary e)
                                             :button (button->key (.-button e))
                                             :buttons (button->key (.-buttons e))
                                             :modifiers (modifiers e)}]))
