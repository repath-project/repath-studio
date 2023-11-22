(ns renderer.reepl.repl-items
  (:require
   ["react" :as react]
   [cljs.reader]
   [cljs.tools.reader]
   [reagent.core :as r]
   [renderer.reepl.codemirror :as codemirror]
   [renderer.reepl.helpers :as helpers]
   [renderer.reepl.show-value :refer [show-value]]))

(def styles
  {:repl-items {:flex 1
                :overflow-y "auto"
                :overflow-x "hidden"
                :height "200px"
                :padding "3px"
                :flex-basis "100%"
                :border-bottom "1px solid var(--border-color)"
                :flex-shrink 1}
   :repl-item {:flex-direction :row
               :font-size 12
               :font-family "Source Code Pro, monospace"
               :padding "3px 5px"}

   :intro-message {:padding "10px 27px"
                   :line-height 1.5
                   :border-bottom "1px solid var(--border-color)"
                   :flex-direction :row}

   :input-item {}
   :output-item {}
   :error-item {:color "var(--error-color)"}
   :underlying-error {:margin-left 10}
   :caret {:color "var(--font-color-disabled)"
           :font-weight "bold"
           :font-size 12
           :flex-direction :row}
   :input-text {:flex 1
                :cursor :pointer
                :word-wrap :break-word}
   :output-caret {}
   :output-value {:flex 1
                  :word-wrap :break-word}})

(def view (partial helpers/view styles))
(def text (partial helpers/text styles))

(defmulti repl-item (fn [item _opts] (:type item)))

(defmethod repl-item :input
  [{:keys [_num text]} opts]
  [view {:style [:repl-item :input-item]}
   [view {:style [:caret]} "=>"]
   [view {:style :input-text
          :on-click (partial (:set-text opts) text)}
    [codemirror/colored-text text]]])

(defmethod repl-item :log
  [{:keys [value]} opts]
  [view {:style [:repl-item :log-item]}
   [show-value value nil opts]])

(defmethod repl-item :error
  [{:keys [value]} _opts]
  (let [message (.-message value)
        underlying (.-cause value)]
    [view {:style [:repl-item :output-item :error-item]}
     message
     (when underlying
       ;; TODO also show stack?
       [text :underlying-error (.-message underlying)])]))

(defmethod repl-item :output
  [{:keys [value]} opts]
  [view {:style [:repl-item :output-item]}
   [view :output-value [show-value value nil opts]]])

(defn repl-items [_]
  (let [ref (react/createRef)]
    (r/create-class
     {:component-did-update
      (fn [_this]
        (let [el (.-current ref)]
          (set! (.-scrollTop el) (.-scrollHeight el))))
      :reagent-render
      (fn [items opts]
        (into
         [view {:style :repl-items
                :ref ref}]
         (map #(repl-item % opts) items)))})))
