(ns repath.studio.keyboard
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp])
  (:import [goog.events KeyCodes]))

(def key-codes
  "SEE https://google.github.io/closure-library/api/goog.events.KeyCodes.html"
  (js->clj KeyCodes))

(rf/dispatch
 [::rp/set-keydown-rules
  {:event-keys [[[:document/toggle-grid]
                 [{:keyCode 35}]
                 [{:keyCode (key-codes "THREE")
                   :shiftKey true}]]
                [[:elements/raise]
                 [{:keyCode (key-codes "PAGE_UP")}]]
                [[:elements/lower]
                 [{:keyCode (key-codes "PAGE_DOWN")}]]
                [[:elements/raise-to-top]
                 [{:keyCode (key-codes "HOME")}]]
                [[:elements/lower-to-bottom]
                 [{:keyCode (key-codes "END")}]]
                [[:pan-to-active-page :original]
                 [{:keyCode (key-codes "ONE")}]]
                [[:pan-to-active-page :fit]
                 [{:keyCode (key-codes "TWO")}]]
                [[:pan-to-active-page :fill]
                 [{:keyCode (key-codes "THREE")}]]
                [[:zoom-in]
                 [{:keyCode (key-codes "EQUALS")}]]
                [[:zoom-out]
                 [{:keyCode (key-codes "DASH")}]]
                [[:window/toggle-tree]
                 [{:keyCode (key-codes "T")
                   :ctrlKey true}]]
                [[:elements/to-path]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:window/toggle-properties]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true}]]
                [[:window/toggle-header]
                 [{:keyCode (key-codes "L")
                   :ctrlKey true}]]
                [[:elements/copy]
                 [{:keyCode (key-codes "C")
                   :ctrlKey true}]]
                [[:elements/paste-styles]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:elements/paste-in-position]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true
                   :altKey true}]]
                [[:elements/paste]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true}]]
                [[:elements/cut]
                 [{:keyCode (key-codes "X")
                   :ctrlKey true}]]
                [[:toggle-debug-info]
                 [{:keyCode (key-codes "D")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:elements/duplicate-in-posistion]
                 [{:keyCode (key-codes "D")
                   :ctrlKey true}]]
                [[:elements/ungroup]
                 [{:keyCode (key-codes "G")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:elements/group]
                 [{:keyCode (key-codes "G")
                   :ctrlKey true}]]
                [[:elements/delete]
                 [{:keyCode (key-codes "DELETE")
                   :shiftKey true}]
                 [{:keyCode (key-codes "DELETE")}]
                 [{:keyCode (key-codes "BACKSPACE")}]]
                [[:document/new]
                 [{:keyCode (key-codes "N")
                   :ctrlKey true}]]
                [[:history/cancel]
                 [{:keyCode (key-codes "ESC")}]]
                [[:history/redo 1]
                 [{:keyCode (key-codes "Z")
                   :ctrlKey true
                   :shiftKey true}]
                 [{:keyCode (key-codes "Y")
                   :ctrlKey true}]]
                [[:history/undo 1]
                 [{:keyCode (key-codes "Z")
                   :ctrlKey true}]]
                [[:elements/deselect-all]
                 [{:keyCode (key-codes "A")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:elements/select-all]
                 [{:keyCode (key-codes "A")
                   :ctrlKey true}]]
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
                [[:set-tool :rect]
                 [{:keyCode (key-codes "R")}]]
                [[:set-tool :fill]
                 [{:keyCode (key-codes "F")}]]
                [[:elements/translate [0 -1]]
                 [{:keyCode (key-codes "UP")}]]
                [[:elements/translate [0 1]]
                 [{:keyCode (key-codes "DOWN")}]]
                [[:elements/translate [-1 0]]
                 [{:keyCode (key-codes "LEFT")}]]
                [[:elements/translate [1 0]]
                 [{:keyCode (key-codes "RIGHT")}]]
                [[:window/close]
                 [{:keyCode (key-codes "F4")
                   :altKey true}]]
                [[:set-command-palette? true]
                 [{:keyCode 186}]]] 

   :clear-keys []

   :always-listen-keys []

   :prevent-default-keys [{:keyCode (key-codes "RIGHT")}
                          {:keyCode (key-codes "LEFT")}
                          {:keyCode (key-codes "UP")}
                          {:keyCode (key-codes "DOWN")}
                          {:keyCode (key-codes "A")
                           :ctrlKey true}]}])