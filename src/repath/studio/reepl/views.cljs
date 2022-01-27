(ns repath.studio.reepl.views
  (:require
   [repath.studio.reepl.core :as reepl]
   [reagent.core :as r]
   [repath.studio.reepl.replumb :as replumb]
   [repath.studio.reepl.show-function :as show-function]
   [repath.studio.reepl.show-devtools :as show-devtools]))

;; Used to make the repl reload-tolerant
(defonce repl-state
  (r/atom reepl/initial-state))

(defn maybe-fn-docs [fn]
  (let [doc (replumb/doc-from-sym fn)]
    (when (:forms doc)
      (with-out-str
        (replumb/print-doc doc)))))

(defn main-view []
  [:div.v-box {:style {:overflow "visible"}}
    [reepl/repl
     :execute #(replumb/run-repl %1 {:warning-as-error true} %2)
     :complete-word replumb/process-apropos
     :get-docs replumb/process-doc
     :state repl-state
     :show-value-opts
     {:showers [show-devtools/show-devtools
                (partial show-function/show-fn-with-docs maybe-fn-docs)]}
     :js-cm-opts {:mode "clojure"
                  :keyMap "default"
                  :showCursorWhenSelecting true}]])
