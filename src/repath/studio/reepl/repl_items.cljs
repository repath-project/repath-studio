(ns repath.studio.reepl.repl-items
  (:require [cljs.reader]
            [cljs.tools.reader]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [replumb.core :as replumb]
            [repath.studio.reepl.show-value :refer [show-value]]
            [repath.studio.reepl.codemirror :as codemirror]
            [repath.studio.reepl.helpers :as helpers]
            [repath.studio.styles :as styles]))

(def styles
  {:repl-items {:flex 1
                :overflow-y "auto"
                :overflow-x "hidden"
                :height "200px"
                :padding "3px"
                :flex-basis "100%"
                :border-bottom (str "1px solid " styles/border-color)
                :flex-shrink 1}
   :repl-item {:flex-direction :row
               :font-size 12
               :font-family "Source Code Pro, monospace"
               :padding "3px 5px"}

   :intro-message {:padding "10px 27px"
                   :line-height 1.5
                   :border-bottom (str "1px solid " styles/border-color)
                   :flex-direction :row}

   :input-item {}
   :output-item {}
   :error-item {:color styles/error-color}
   :underlying-error {:margin-left 10}
   :caret {:color styles/font-color-disabled
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
(def button (partial helpers/button styles))

(defmulti repl-item (fn [item opts] (:type item)))

(defmethod repl-item :input
  [{:keys [num text]} opts]
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
  [{:keys [value]} opts]
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
  (r/create-class
   {:component-did-update
    (fn [this]
      (let [el (dom/dom-node this)]
        (set! (.-scrollTop el) (.-scrollHeight el))))
    :reagent-render
    (fn [items opts]
      (into
       [view :repl-items]
       (map #(repl-item % opts) items)))}))
