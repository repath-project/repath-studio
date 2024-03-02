(ns renderer.utils.keyboard
  (:require
   [clojure.set :as set]
   [re-frame.core :as rf])
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
  "https://day8.github.io/re-frame/FAQs/Null-Dispatched-Events/"
  [e]
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
  [e v f & more]
  (let [target (.-target e)]
    (.stopPropagation e)
    (case (.-code e)
      "Enter" (do (apply f e more)
                  (.blur target))
      "Escape" (do (set! (.-value target) v)
                   (.blur target))
      nil)))

(def keydown-rules
  {:event-keys [[[:document/toggle-grid]
                 [{:keyCode 35}]
                 [{:keyCode (key-codes "THREE")
                   :shiftKey true}]]
                [[:element/raise]
                 [{:keyCode (key-codes "PAGE_UP")}]]
                [[:element/lower]
                 [{:keyCode (key-codes "PAGE_DOWN")}]]
                [[:element/raise-to-top]
                 [{:keyCode (key-codes "HOME")}]]
                [[:element/lower-to-bottom]
                 [{:keyCode (key-codes "END")}]]
                [[:focus-selection :original]
                 [{:keyCode (key-codes "ONE")}]]
                [[:focus-selection :fit]
                 [{:keyCode (key-codes "TWO")}]]
                [[:focus-selection :fill]
                 [{:keyCode (key-codes "THREE")}]]
                [[:zoom-in]
                 [{:keyCode (key-codes "EQUALS")}]]
                [[:zoom-out]
                 [{:keyCode (key-codes "DASH")}]]
                [[:element/->path]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:panel/toggle :tree]
                 [{:keyCode (key-codes "T")
                   :ctrlKey true}]]
                [[:panel/toggle :properties]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true}]]
                [[:element/stroke->path]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true
                   :altKey true}]]
                [[:element/copy]
                 [{:keyCode (key-codes "C")
                   :ctrlKey true}]]
                [[:element/paste-styles]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/paste-in-place]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true
                   :altKey true}]]
                [[:element/paste]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true}]]
                [[:element/cut]
                 [{:keyCode (key-codes "X")
                   :ctrlKey true}]]
                [[:toggle-debug-info]
                 [{:keyCode (key-codes "D")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/duplicate-in-place]
                 [{:keyCode (key-codes "D")
                   :ctrlKey true}]]
                [[:element/bool-operation :exclude]
                 [{:keyCode (key-codes "E")
                   :ctrlKey true}]]
                [[:element/bool-operation :unite]
                 [{:keyCode (key-codes "U")
                   :ctrlKey true}]]
                [[:element/bool-operation :intersect]
                 [{:keyCode (key-codes "I")
                   :ctrlKey true}]]
                #_[[:element/bool-operation :subtract]
                   [{:keyCode (key-codes "S")
                     :ctrlKey true}]]
                [[:element/bool-operation :divide]
                 [{:keyCode (key-codes "/")
                   :ctrlKey true}]]
                [[:element/ungroup]
                 [{:keyCode (key-codes "G")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/group]
                 [{:keyCode (key-codes "G")
                   :ctrlKey true}]]
                [[:element/unlock]
                 [{:keyCode (key-codes "L")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/lock]
                 [{:keyCode (key-codes "L")
                   :ctrlKey true}]]
                [[:element/delete]
                 [{:keyCode (key-codes "DELETE")}]
                 [{:keyCode (key-codes "BACKSPACE")}]]
                [[:document/new]
                 [{:keyCode (key-codes "N")
                   :ctrlKey true}]]
                [[:history/cancel]
                 [{:keyCode (key-codes "ESC")}]]
                [[:history/redo]
                 [{:keyCode (key-codes "Z")
                   :ctrlKey true
                   :shiftKey true}]
                 [{:keyCode (key-codes "Y")
                   :ctrlKey true}]]
                [[:history/undo]
                 [{:keyCode (key-codes "Z")
                   :ctrlKey true}]]
                [[:element/select-same-tags]
                 [{:keyCode (key-codes "A")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/select-all]
                 [{:keyCode (key-codes "A")
                   :ctrlKey true}]]
                [[:menubar/focus "file"]
                 [{:keyCode (key-codes "F")
                   :altKey true}]]
                [[:menubar/focus "edit"]
                 [{:keyCode (key-codes "E")
                   :altKey true}]]
                [[:menubar/focus "object"]
                 [{:keyCode (key-codes "O")
                   :altKey true}]]
                [[:menubar/focus "view"]
                 [{:keyCode (key-codes "V")
                   :altKey true}]]
                [[:menubar/focus "help"]
                 [{:keyCode (key-codes "H")
                   :altKey true}]]
                [[:element/move-up]
                 [{:keyCode (key-codes "UP")}]]
                [[:element/move-down]
                 [{:keyCode (key-codes "DOWN")}]]
                [[:element/move-left]
                 [{:keyCode (key-codes "LEFT")}]]
                [[:element/move-right]
                 [{:keyCode (key-codes "RIGHT")}]]
                [[:window/close]
                 [{:keyCode (key-codes "Q")
                   :ctrlKey true}]]
                [[:document/open]
                 [{:keyCode (key-codes "O")
                   :ctrlKey true}]]
                [[:document/save]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true}]]
                [[:document/save-as]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:document/save-all]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true
                   :altKey true}]]
                [[:document/close-active]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true}]]
                [[:document/close-all]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true
                   :altKey true}]]
                [[:document/close-others]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:window/toggle-fullscreen]
                 [{:keyCode (key-codes "F11")}]]
                [[:cmdk/toggle]
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
                          {:keyCode (key-codes "F11")}
                          {:keyCode (key-codes "A")
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
