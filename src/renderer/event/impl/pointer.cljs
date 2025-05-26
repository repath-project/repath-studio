(ns renderer.event.impl.pointer
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.element.db :refer [Element]]
   [renderer.event.db :refer [PointerEvent PointerButton]]
   [renderer.event.events :as-alias event.events]
   [renderer.tool.db :refer [Handle]]))

(m/=> button->key [:-> [:enum -1 0 1 2 3 4] [:maybe PointerButton]])
(defn button->key
  "https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/button"
  [button]
  (get {0 :left
        1 :middle
        2 :right
        3 :back
        4 :forward} button))

(m/=> ->clj [:-> any? [:maybe [:or Element Handle]] PointerEvent])
(defn ->clj
  "https://developer.mozilla.org/en-US/docs/Web/API/PointerEvent"
  [^js/PointerEvent e el]
  {:element el
   :target (.-target e)
   :type (.-type e)
   :pointer-pos [(.-pageX e) (.-pageY e)]
   :pressure (.-pressure e)
   :pointer-type (.-pointerType e)
   :pointer-id (.-pointerId e)
   :timestamp (.-timeStamp e)
   :primary (.-isPrimary e)
   :button (button->key (.-button e))
   :alt-key (.-altKey e)
   :ctrl-key (.-ctrlKey e)
   :meta-key (.-metaKey e)
   :shift-key (.-shiftKey e)})

(m/=> handler! [:-> any? [:or Element Handle] nil?])
(defn handler!
  "Gathers pointer event props and dispathces the corresponding event.
   https://day8.github.io/re-frame/FAQs/Null-Dispatched-Events/"
  [^js/PointerEvent e el]
  (.stopPropagation e)

  ;; REVIEW
  (when (= (.-pointerType e) "touch")
    (.preventDefault e))

  ;; Although the fps might drop because synced dispatch blocks rendering,
  ;; the end result appears to be more responsive because it's synced with the
  ;; pointer movement.
  (rf/dispatch-sync [::event.events/pointer (->clj e el)]))
