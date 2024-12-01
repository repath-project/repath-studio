(ns renderer.utils.pointer
  (:require
   [clojure.core.matrix :as mat]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.document.db :refer [ZoomFactor]]
   [renderer.element.db :refer [Element]]
   [renderer.tool.db :refer [Handle]]
   [renderer.tool.events :as-alias tool.e]
   [renderer.utils.keyboard :refer [ModifierKey modifiers]]
   [renderer.utils.math :refer [Vec2]]))

(def PointerButton [:enum :left :middle :right :back :forward])

(def PointerEvent
  [:map {:closed true}
   [:element [:maybe [:or Element Handle]]]
   [:target any?]
   [:pointer-pos [:maybe Vec2]]
   [:pressure [:maybe number?]]
   [:pointer-type [:enum "mouse" "pen" "touch"]]
   [:pointer-id number?]
   [:primary boolean?]
   [:button [:maybe PointerButton]]
   [:modifiers [:set ModifierKey]]
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

(m/=> adjusted-position [:-> ZoomFactor Vec2 Vec2 Vec2])
(defn adjusted-position
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

(m/=> lock-direction [:-> Vec2 Vec2])
(defn lock-direction
  "Locks pointer movement to the axis with the biggest offset"
  [[x y]]
  (if (> (abs x) (abs y))
    [x 0]
    [0 y]))

(m/=> event-handler! [:-> any? [:or Element Handle] nil?])
(defn event-handler!
  "Gathers pointer event props.
   https://developer.mozilla.org/en-US/docs/Web/API/PointerEvent

   Then dispathces the corresponding event.
   https://day8.github.io/re-frame/FAQs/Null-Dispatched-Events/"
  [^js/PointerEvent e el]
  (.stopPropagation e)

  (when (= (.-pointerType e) "touch")
    (.preventDefault e))

  ;; Although the fps might drop because synced dispatch blocks rendering,
  ;; the end result appears to be more responsive because it's synced with the
  ;; pointer movement.
  (rf/dispatch-sync [::tool.e/pointer-event {:element el
                                             :target (.-target e)
                                             :type (.-type e)
                                             :pointer-pos [(.-pageX e) (.-pageY e)]
                                             :pressure (.-pressure e)
                                             :pointer-type (.-pointerType e)
                                             :pointer-id (.-pointerId e)
                                             :primary (.-isPrimary e)
                                             :button (button->key (.-button e))
                                             :modifiers (modifiers e)}]))
