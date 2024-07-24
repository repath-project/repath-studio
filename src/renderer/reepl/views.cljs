(ns renderer.reepl.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [renderer.reepl.core :as reepl]
   [renderer.reepl.replumb :as replumb]
   [renderer.reepl.show-devtools :as show-devtools]
   [renderer.reepl.show-function :as show-function]))

;; Used to make the repl reload-tolerant
(defonce repl-state
  (r/atom reepl/initial-state))

(defn maybe-fn-docs
  [fn]
  (let [doc (replumb/doc-from-sym fn)]
    (when (:forms doc)
      (with-out-str
        (replumb/print-doc doc)))))

(defn root
  []
  [reepl/repl
   :execute #(replumb/run-repl (case @(rf/subscribe [:repl-mode])
                                 :cljs %1
                                 :js (str "(js/eval \"" %1 "\")")
                                 :py (str "(js/window.api.runPython \"" %1 "\")"))
                               {:warning-as-error true} %2)
   :complete-word (fn [text] (replumb/process-apropos @(rf/subscribe [:repl-mode]) text))
   :get-docs (if (= @(rf/subscribe [:repl-mode]) :cljs) replumb/process-doc #())
   :state repl-state
   :show-value-opts
   {:showers [show-devtools/show-devtools
              (partial show-function/show-fn-with-docs maybe-fn-docs)]}
   :js-cm-opts {:mode (case
                       :cljs "clojure"
                       :js "javascript"
                       :py "python")
                :keyMap "default"
                :showCursorWhenSelecting true}])
