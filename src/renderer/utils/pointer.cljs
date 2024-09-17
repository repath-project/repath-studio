(ns renderer.utils.pointer
  (:require
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.element.db :refer [Element Handle]]
   [renderer.utils.keyboard :refer [ModifierKey]]
   [renderer.utils.math :refer [Vec2D]]))

(def PointerButton [:enum :left :middle :right :back :forward])

(def PointerEvent
  [:map {:closed true}
   [:element [:maybe [:or Element Handle]]]
   [:target any?]
   [:type [:enum "dblclick" "pointerover" "pointerenter" "pointerdown" "pointermove" "pointerrawupdate" "pointerup" "pointercancel" "pointerout" "pointerleave" "gotpointercapture" "lostpointercapture"]]
   [:pointer-pos [:maybe Vec2D]]
   [:pressure [:maybe number?]]
   [:pointer-type [:maybe [:enum "mouse" "pen" "touch"]]]
   [:pointer-id number?]
   [:primary? boolean?]
   [:altitude [:maybe number?]]
   [:azimuth [:maybe number?]]
   [:twist [:maybe number?]]
   [:tilt [:maybe Vec2D]]
   [:button [:maybe PointerButton]]
   [:buttons [:maybe PointerButton]]
   [:delta [:maybe Vec2D]]
   [:modifiers [:set ModifierKey]]])

(mx/defn ctrl? :- boolean?
  [e]
  (contains? (:modifiers e) :ctrl))

(mx/defn shift? :- boolean?
  [e]
  (contains? (:modifiers e) :shift))

(mx/defn alt? :- boolean?
  [e]
  (contains? (:modifiers e) :alt))

(mx/defn significant-drag? :- boolean?
  [position :- Vec2D, offset :- Vec2D, threshold :- number?]
  (> (apply max (map abs (mat/sub position offset)))
     threshold))

(mx/defn adjust-position :- Vec2D
  [zoom :- number?, pan :- Vec2D, pointer-pos :- Vec2D]
  (-> pointer-pos
      (mat/div zoom)
      (mat/add pan)))

(mx/defn button->key :- [:maybe PointerButton]
  "https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/button"
  [button :- number?]
  (get {0 :left
        1 :middle
        2 :right
        3 :back
        4 :forward} button))

(mx/defn lock-direction :- Vec2D
  "Locks pointer movement to the axis with the biggest offset"
  [[x y] :- Vec2D]
  (if (> (abs x) (abs y))
    [x 0]
    [0 y]))

(defn event-handler
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

  (rf/dispatch-sync [::app.e/pointer-event {:element el
                                            :target (.-target e)
                                            :type (.-type e)
                                            :pointer-pos [(.-pageX e) (.-pageY e)]
                                            :pressure (.-pressure e)
                                            :pointer-type (.-pointerType e)
                                            :pointer-id (.-pointerId e)
                                            :primary? (.-isPrimary e)
                                            :altitude (.-altitudeAngle e)
                                            :azimuth (.-azimuthAngle e)
                                            :twist (.-twist e)
                                            :tilt (when (and (.-tiltX e) (.-tiltY e))
                                                    [(.-tiltX e) (.-tiltY e)])
                                            :button (button->key (.-button e))
                                            :buttons (button->key (.-buttons e))
                                            :modifiers (cond-> #{}
                                                         (.-altKey e) (conj :alt)
                                                         (.-ctrlKey e) (conj :ctrl)
                                                         (.-metaKey e) (conj :meta)
                                                         (.-shiftKey e) (conj :shift))}]))
