(ns renderer.reepl.views
  (:require
   [renderer.reepl.core :as reepl]
   [reagent.core :as r]
   [renderer.reepl.replumb :as replumb]
   [renderer.reepl.show-function :as show-function]
   [renderer.reepl.show-devtools :as show-devtools]
   [re-frame.core :as rf]))

;; Used to make the repl reload-tolerant
(defonce repl-state
  (r/atom reepl/initial-state))

(defn maybe-fn-docs [fn]
  (let [doc (replumb/doc-from-sym fn)]
    (when (:forms doc)
      (with-out-str
        (replumb/print-doc doc)))))

(defn main-view []
  [:div.flex.flex-col {:style {:overflow "visible"}}
   [reepl/repl
    :execute #(replumb/run-repl (if (= @(rf/subscribe [:repl-mode]) :cljs) %1 (str "(js/eval \"" %1 "\")")) {:warning-as-error true} %2)
    :complete-word replumb/process-apropos
    :get-docs replumb/process-doc
    :state repl-state
    :show-value-opts
    {:showers [show-devtools/show-devtools
               (partial show-function/show-fn-with-docs maybe-fn-docs)]}
    :js-cm-opts {:mode (if (= @(rf/subscribe [:repl-mode]) :cljs) "clojure" "javascript")
                 :keyMap "default"
                 :showCursorWhenSelecting true}]])
