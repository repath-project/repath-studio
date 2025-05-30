(ns renderer.event.impl.keyboard
  (:require
   [clojure.set :as set]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.document.events :as-alias document.events]
   [renderer.element.events :as-alias element.events]
   [renderer.event.db :refer [KeyboardEvent]]
   [renderer.event.events :as-alias event.events]
   [renderer.events :as-alias events]
   [renderer.frame.events :as-alias frame.events]
   [renderer.history.events :as-alias history.events]
   [renderer.tool.events :as-alias tool.events]
   [renderer.window.events :as-alias window.events])
  (:import
   [goog.events KeyCodes]))

;; https://google.github.io/closure-library/api/goog.events.KeyCodes.html
(def key-codes
  (js->clj KeyCodes))

(def key-chars
  (set/map-invert key-codes))

(m/=> key-code->key [:-> number? [:maybe string?]])
(defn key-code->key
  [key-code]
  (get key-chars key-code))

(m/=> ->clj [:-> any? KeyboardEvent])
(defn ->clj
  "https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent"
  [^js/KeyboardEvent e]
  {:target (.-target e)
   :type (.-type e)
   :code (.-code e)
   :key-code (.-keyCode e)
   :key (.-key e)
   :alt-key (.-altKey e)
   :ctrl-key (.-ctrlKey e)
   :meta-key (.-metaKey e)
   :shift-key (.-shiftKey e)})

(defn handler!
  [^js/KeyboardEvent e]
  (rf/dispatch-sync [::event.events/keyboard (->clj e)]))

(defn input-key-down-handler!
  "Generic on-key-down handler for input elements that dispatches an event `f`
   in order to update a db value on keyboard enter, or reset to the initial
   value `v` on escape.

   We need this to avoid updating the canvas with incomplete values while the
   user is typing, and also avoid polluting the history stack.

   The `default-value` attribute should be used to update the value reactively."
  [^js/KeyboardEvent e v f & more]
  (let [target (.-target e)]
    (.stopPropagation e)

    (case (.-code e)
      "Enter" (do (apply f e more)
                  (.blur target))
      "Escape" (do (set! (.-value target) v)
                   (.blur target))
      nil)))

(def keydown-rules
  {:event-keys [[[::element.events/raise]
                 [{:keyCode (key-codes "PAGE_UP")}]]
                [[::element.events/lower]
                 [{:keyCode (key-codes "PAGE_DOWN")}]]
                [[::element.events/raise-to-top]
                 [{:keyCode (key-codes "HOME")}]]
                [[::element.events/lower-to-bottom]
                 [{:keyCode (key-codes "END")}]]
                [[::frame.events/focus-selection :original]
                 [{:keyCode (key-codes "ONE")}]]
                [[::frame.events/focus-selection :fit]
                 [{:keyCode (key-codes "TWO")}]]
                [[::frame.events/focus-selection :fill]
                 [{:keyCode (key-codes "THREE")}]]
                [[::frame.events/zoom-in]
                 [{:keyCode (key-codes "EQUALS")}]]
                [[::frame.events/zoom-out]
                 [{:keyCode (key-codes "DASH")}]]
                [[::element.events/->path]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::app.events/toggle-panel :tree]
                 [{:keyCode (key-codes "T")
                   :ctrlKey true}]]
                [[::app.events/toggle-panel :properties]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true}]]
                [[::element.events/stroke->path]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true
                   :altKey true}]]
                [[::element.events/copy]
                 [{:keyCode (key-codes "C")
                   :ctrlKey true}]]
                [[::element.events/paste-styles]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/paste-in-place]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true
                   :altKey true}]]
                [[::element.events/paste]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true}]]
                [[::element.events/cut]
                 [{:keyCode (key-codes "X")
                   :ctrlKey true}]]
                [[::app.events/toggle-debug-info]
                 [{:keyCode (key-codes "D")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/duplicate]
                 [{:keyCode (key-codes "D")
                   :ctrlKey true}]]
                [[::element.events/boolean-operation :exclude]
                 [{:keyCode (key-codes "E")
                   :ctrlKey true}]]
                [[::element.events/boolean-operation :unite]
                 [{:keyCode (key-codes "U")
                   :ctrlKey true}]]
                [[::element.events/boolean-operation :intersect]
                 [{:keyCode (key-codes "I")
                   :ctrlKey true}]]
                [[::element.events/boolean-operation :subtract]
                 [{:keyCode (key-codes "BACKSLASH")
                   :ctrlKey true}]]
                [[::element.events/boolean-operation :divide]
                 [{:keyCode (key-codes "SLASH")
                   :ctrlKey true}]]
                [[::element.events/ungroup]
                 [{:keyCode (key-codes "G")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/group]
                 [{:keyCode (key-codes "G")
                   :ctrlKey true}]]
                [[::element.events/unlock]
                 [{:keyCode (key-codes "L")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/lock]
                 [{:keyCode (key-codes "L")
                   :ctrlKey true}]]
                [[::element.events/delete]
                 [{:keyCode (key-codes "DELETE")}]
                 [{:keyCode (key-codes "BACKSPACE")}]]
                [[::document.events/new]
                 [{:keyCode (key-codes "N")
                   :ctrlKey true}]]
                [[::tool.events/cancel]
                 [{:keyCode (key-codes "ESC")}]]
                [[::history.events/redo]
                 [{:keyCode (key-codes "Z")
                   :ctrlKey true
                   :shiftKey true}]
                 [{:keyCode (key-codes "Y")
                   :ctrlKey true}]]
                [[::history.events/undo]
                 [{:keyCode (key-codes "Z")
                   :ctrlKey true}]]
                [[::element.events/select-same-tags]
                 [{:keyCode (key-codes "A")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.events/select-all]
                 [{:keyCode (key-codes "A")
                   :ctrlKey true}]]
                [[::events/focus "file"]
                 [{:keyCode (key-codes "F")
                   :altKey true}]]
                [[::events/focus "edit"]
                 [{:keyCode (key-codes "E")
                   :altKey true}]]
                [[::events/focus "object"]
                 [{:keyCode (key-codes "O")
                   :altKey true}]]
                [[::events/focus "view"]
                 [{:keyCode (key-codes "V")
                   :altKey true}]]
                [[::events/focus "help"]
                 [{:keyCode (key-codes "H")
                   :altKey true}]]
                [[::window.events/close]
                 [{:keyCode (key-codes "Q")
                   :ctrlKey true}]]
                [[::document.events/open nil]
                 [{:keyCode (key-codes "O")
                   :ctrlKey true}]]
                [[::document.events/save-as]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::document.events/save]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true}]]
                [[::document.events/close-active]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true}]]
                [[::document.events/close-all]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true
                   :altKey true}]]
                [[::window.events/toggle-fullscreen]
                 [{:keyCode (key-codes "F11")}]]
                [[::dialog.events/show-cmdk]
                 [{:keyCode (key-codes "F1")}]
                 [{:keyCode (key-codes "K")
                   :ctrlKey true}]]
                [[::tool.events/activate :edit]
                 [{:keyCode (key-codes "E")}]]
                [[::tool.events/activate :circle]
                 [{:keyCode (key-codes "C")}]]
                [[::tool.events/activate :line]
                 [{:keyCode (key-codes "L")}]]
                [[::tool.events/activate :text]
                 [{:keyCode (key-codes "T")}]]
                [[::tool.events/activate :pan]
                 [{:keyCode (key-codes "P")}]]
                [[::tool.events/activate :zoom]
                 [{:keyCode (key-codes "Z")}]]
                [[::tool.events/activate :rect]
                 [{:keyCode (key-codes "R")}]]
                [[::tool.events/activate :transform]
                 [{:keyCode (key-codes "S")}]]
                [[::tool.events/activate :fill]
                 [{:keyCode (key-codes "F")}]]]

   :clear-keys []

   :always-listen-keys []

   :prevent-default-keys [{:keyCode (key-codes "EQUALS")}
                          {:keyCode (key-codes "DASH")}
                          {:keyCode (key-codes "RIGHT")}
                          {:keyCode (key-codes "LEFT")}
                          {:keyCode (key-codes "UP")}
                          {:keyCode (key-codes "DOWN")}
                          {:keyCode (key-codes "F1")}
                          {:keyCode (key-codes "F11")}
                          {:keyCode (key-codes "A")
                           :ctrlKey true}
                          {:keyCode (key-codes "O")
                           :ctrlKey true}
                          {:keyCode (key-codes "S")
                           :ctrlKey true}
                          {:keyCode (key-codes "G")
                           :ctrlKey true}
                          {:keyCode (key-codes "P")
                           :ctrlKey true}
                          {:keyCode (key-codes "W")
                           :ctrlKey true}
                          {:keyCode (key-codes "K")
                           :ctrlKey true}
                          {:keyCode (key-codes "W")
                           :ctrlKey true}
                          {:keyCode (key-codes "D")
                           :ctrlKey true
                           :shiftKey true}]})
