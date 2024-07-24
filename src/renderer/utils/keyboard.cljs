(ns renderer.utils.keyboard
  (:require
   [clojure.set :as set]
   [re-frame.core :as rf]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.document.events :as-alias document.e]
   [renderer.history.events :as-alias history.e]
   [renderer.element.events :as-alias element.e]
   [renderer.frame.events :as-alias frame.e]
   [renderer.window.events :as-alias window.e])
  (:import
   [goog.events KeyCodes]))

(def key-codes
  "https://google.github.io/closure-library/api/goog.events.KeyCodes.html"
  (js->clj KeyCodes))

(def key-chars
  (set/map-invert key-codes))

(defn code->key
  [code]
  (get key-chars code))

(defn event-handler
  "https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent
   https://day8.github.io/re-frame/FAQs/Null-Dispatched-Events/"
  [^js/KeyboardEvent e]
  (rf/dispatch-sync [:keyboard-event
                     {:target (.-target e)
                      :type (keyword (.-type e))
                      :code (.-code e)
                      :key-code (.-keyCode e)
                      :key (.-key e)
                      :modifiers (cond-> #{}
                                   (.-altKey e) (conj :alt)
                                   (.-ctrlKey e) (conj :ctrl)
                                   (.-metaKey e) (conj :meta)
                                   (.-shiftKey e) (conj :shift))}]))

(defn input-key-down-handler
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
  {:event-keys [[[::element.e/raise]
                 [{:keyCode (key-codes "PAGE_UP")}]]
                [[::element.e/lower]
                 [{:keyCode (key-codes "PAGE_DOWN")}]]
                [[::element.e/raise-to-top]
                 [{:keyCode (key-codes "HOME")}]]
                [[::element.e/lower-to-bottom]
                 [{:keyCode (key-codes "END")}]]
                [[::frame.e/focus-selection :original]
                 [{:keyCode (key-codes "ONE")}]]
                [[::frame.e/focus-selection :fit]
                 [{:keyCode (key-codes "TWO")}]]
                [[::frame.e/focus-selection :fill]
                 [{:keyCode (key-codes "THREE")}]]
                [[::frame.e/zoom-in]
                 [{:keyCode (key-codes "EQUALS")}]]
                [[::frame.e/zoom-out]
                 [{:keyCode (key-codes "DASH")}]]
                [[::element.e/->path]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:panel/toggle :tree]
                 [{:keyCode (key-codes "T")
                   :ctrlKey true}]]
                [[:panel/toggle :properties]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true}]]
                [[::element.e/stroke->path]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true
                   :altKey true}]]
                [[::element.e/copy]
                 [{:keyCode (key-codes "C")
                   :ctrlKey true}]]
                [[::element.e/paste-styles]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.e/paste-in-place]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true
                   :altKey true}]]
                [[::element.e/paste]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true}]]
                [[::element.e/cut]
                 [{:keyCode (key-codes "X")
                   :ctrlKey true}]]
                [[:toggle-debug-info]
                 [{:keyCode (key-codes "D")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.e/duplicate-in-place]
                 [{:keyCode (key-codes "D")
                   :ctrlKey true}]]
                [[::element.e/bool-operation :exclude]
                 [{:keyCode (key-codes "E")
                   :ctrlKey true}]]
                [[::element.e/bool-operation :unite]
                 [{:keyCode (key-codes "U")
                   :ctrlKey true}]]
                [[::element.e/bool-operation :intersect]
                 [{:keyCode (key-codes "I")
                   :ctrlKey true}]]
                [[::element.e/bool-operation :subtract]
                 [{:keyCode (key-codes "BACKSLASH")
                   :ctrlKey true}]]
                [[::element.e/bool-operation :divide]
                 [{:keyCode (key-codes "SLASH")
                   :ctrlKey true}]]
                [[::element.e/ungroup]
                 [{:keyCode (key-codes "G")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.e/group]
                 [{:keyCode (key-codes "G")
                   :ctrlKey true}]]
                [[::element.e/unlock]
                 [{:keyCode (key-codes "L")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.e/lock]
                 [{:keyCode (key-codes "L")
                   :ctrlKey true}]]
                [[::element.e/delete]
                 [{:keyCode (key-codes "DELETE")}]
                 [{:keyCode (key-codes "BACKSPACE")}]]
                [[::document.e/new]
                 [{:keyCode (key-codes "N")
                   :ctrlKey true}]]
                [[::history.e/cancel]
                 [{:keyCode (key-codes "ESC")}]]
                [[::history.e/redo]
                 [{:keyCode (key-codes "Z")
                   :ctrlKey true
                   :shiftKey true}]
                 [{:keyCode (key-codes "Y")
                   :ctrlKey true}]]
                [[::history.e/undo]
                 [{:keyCode (key-codes "Z")
                   :ctrlKey true}]]
                [[::element.e/select-same-tags]
                 [{:keyCode (key-codes "A")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::element.e/select-all]
                 [{:keyCode (key-codes "A")
                   :ctrlKey true}]]
                [[:focus "file"]
                 [{:keyCode (key-codes "F")
                   :altKey true}]]
                [[:focus "edit"]
                 [{:keyCode (key-codes "E")
                   :altKey true}]]
                [[:focus "object"]
                 [{:keyCode (key-codes "O")
                   :altKey true}]]
                [[:focus "view"]
                 [{:keyCode (key-codes "V")
                   :altKey true}]]
                [[:focus "help"]
                 [{:keyCode (key-codes "H")
                   :altKey true}]]
                [[::element.e/move-up]
                 [{:keyCode (key-codes "UP")}]]
                [[::element.e/move-down]
                 [{:keyCode (key-codes "DOWN")}]]
                [[::element.e/move-left]
                 [{:keyCode (key-codes "LEFT")}]]
                [[::element.e/move-right]
                 [{:keyCode (key-codes "RIGHT")}]]
                [[::window.e/close]
                 [{:keyCode (key-codes "Q")
                   :ctrlKey true}]]
                [[::document.e/open]
                 [{:keyCode (key-codes "O")
                   :ctrlKey true}]]
                [[::document.e/save-as]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::document.e/save]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true}]]
                [[::document.e/close-active]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true}]]
                [[::document.e/close-all]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true
                   :altKey true}]]
                [[::document.e/close-others]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true
                   :shiftKey true}]]
                [[::window.e/toggle-fullscreen]
                 [{:keyCode (key-codes "F11")}]]
                [[::dialog.e/cmdk]
                 [{:keyCode (key-codes "F1")}]
                 [{:keyCode (key-codes "K")
                   :ctrlKey true}]]
                [[:set-tool :edit]
                 [{:keyCode (key-codes "E")}]]
                [[:set-tool :circle]
                 [{:keyCode (key-codes "C")}]]
                [[:set-tool :line]
                 [{:keyCode (key-codes "L")}]]
                [[:set-tool :text]
                 [{:keyCode (key-codes "T")}]]
                [[:set-tool :pan]
                 [{:keyCode (key-codes "P")}]]
                [[:set-tool :zoom]
                 [{:keyCode (key-codes "Z")}]]
                [[:set-tool :rect]
                 [{:keyCode (key-codes "R")}]]
                [[:set-tool :select]
                 [{:keyCode (key-codes "S")}]]
                [[:set-tool :fill]
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
