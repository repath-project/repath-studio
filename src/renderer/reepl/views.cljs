(ns renderer.reepl.views
  (:require
   ["react-resizable-panels" :refer [Panel]]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.events :as-alias events]
   [renderer.reepl.codemirror :as codemirror]
   [renderer.reepl.db :as db]
   [renderer.reepl.handlers :as reepl.handlers]
   [renderer.reepl.replumb :as reepl.replumb]
   [renderer.reepl.show-devtools :as show-devtools]
   [renderer.reepl.show-function :as show-function]
   [renderer.reepl.show-value :refer [show-value]]
   [renderer.reepl.subs :as reepl.subs]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]
   [replumb.core :as replumb])
  (:require-macros
   [reagent.ratom :refer [reaction]]))

(defn mode-button
  [mode]
  (let [repl-mode @(rf/subscribe [::app.subs/repl-mode])
        active (= repl-mode mode)]
    [:button.button.rounded.px-1.leading-none.text-2xs.h-4
     {:class [(when active "accent")
              "m-0.5"]
      :on-click #(rf/dispatch [::app.events/set-repl-mode mode])}
     mode]))

(defn repl-input
  [state submit cm-opts]
  {:pre [(every? (comp not nil?)
                 (map cm-opts
                      [:on-up
                       :on-down
                       :complete-atom
                       :complete-word
                       :on-change]))]}
  (let [{:keys [_pos _count _text]} @state
        repl-history? @(rf/subscribe [::app.subs/panel-visible? :repl-history])]
    [:div.flex.p-0.5.items-center.m-1
     [:div.flex.text-xs.self-start
      {:class "m-0.5"}
      (replumb/get-prompt)]
     ^{:key (str (hash (:js-cm-opts cm-opts)))}
     [codemirror/code-mirror
      (reaction (:text @state))
      (merge {:on-eval submit} cm-opts)]
     [:div.self-start.h-full.flex.items-center
      [mode-button :cljs]
      [mode-button :js]]
     [:div.self-start.flex
      [views/icon-button
       (if repl-history? "chevron-down" "chevron-up")
       {:class "my-0.5 ml-0.5"
        :style {:height "16px"}
        :title (if repl-history?
                 (t [::hide-command-output "Hide command output"])
                 (t [::show-command-output "Show command output"]))
        :on-click #(rf/dispatch [::app.events/toggle-panel :repl-history])}]]]))

(defmulti item (fn [i _opts] (:type i)))

(defmethod item :input
  [{:keys [_num text]} opts]
  [:div.text-foreground-disabled.font-bold "=>"]
  [:div.flex-1.cursor-pointer.break-words
   {:on-click #((:set-text opts) text)}
   [codemirror/colored-text text]])

(defmethod item :log
  [{:keys [value]} opts]
  [show-value value nil opts])

(defmethod item :error
  [{:keys [value]} _opts]
  (let [message (.-message value)
        underlying (.-cause value)]
    [:span.text-error
     message
     (when underlying
       ;; TODO: also show stack?
       [:span.ml-2.5 (.-message underlying)])]))

(defmethod item :output
  [{:keys [value]} opts]
  [:div.flex-1.break-words [show-value value nil opts]])

(defn repl-items [items opts]
  [:div.flex-1.border-b.border-border.h-full.overflow-hidden.flex
   [views/scroll-area
    {:ref (fn [this]
            (when this
              (rf/dispatch [::events/scroll-to-bottom this])))}
    (into
     [:div.p-1 {:dir "ltr"}]
     (map (fn [i]
            [:div.font-mono.p-1.flex.text-xs.min-h-4 (item i opts)]) items))]])

(defn repl-items-panel
  [items show-value-opts set-text]
  [:<>
   [views/resize-handle "repl-resize-handle"]
   [:> Panel
    {:id "repl-panel"
     :minSize 10
     :defaultSize 20
     :order 3}
    [repl-items items (assoc show-value-opts :set-text set-text)]]])

(defn completion-item
  [text selected active set-active]
  [:div.p-1.bg-secondary.text-nowrap
   {:ref (fn [this]
           (when (and this selected)
             (rf/dispatch [::events/scroll-into-view this])))
    :on-click set-active
    :class (when selected (if active "bg-accent!" "bg-primary!"))}
   text])

(defn completion-list
  [docs {:keys [pos words active show-all]} set-active]
  (let [items (map-indexed
               #(vector completion-item
                        (get %2 2)
                        (= %1 pos)
                        active
                        (partial set-active %1)) words)]
    [:div.absolute.bottom-full.left-0.w-full.text-xs.mb-px
     (when docs [:div.bg-primary.drop-shadow.p-4.absolute.bottom-full docs])
     (into
      [:div.overflow-hidden.flex
       {:class (when show-all "flex-wrap")}]
      items)]))

(defn maybe-fn-docs
  [f]
  (let [doc (reepl.replumb/doc-from-sym f)]
    (when (:forms doc)
      (with-out-str
        (reepl.replumb/print-doc doc)))))

(defn set-print!
  [log]
  (set! cljs.core/*print-newline* false)
  (set! cljs.core/*print-err-fn*
        (fn [& args]
          (if (= 1 (count args))
            (log (first args))
            (log args))))
  (set! cljs.core/*print-fn*
        (fn [& args]
          (if (= 1 (count args))
            (log (first args))
            (log args)))))

(defn repl
  [& {:keys [execute
             complete-word
             get-docs
             state
             show-value-opts
             js-cm-opts
             on-cm-init]}]
  (reagent/with-let [state (or state (reagent/atom db/initial-state))
                     {:keys [add-input
                             add-result
                             go-up
                             go-down
                             clear-items
                             set-text
                             add-log]} (reepl.handlers/make-handlers state)
                     items (reepl.subs/items state)
                     complete-atom (reagent/atom nil)
                     docs (reaction
                           (when-let [state @complete-atom]
                             (let [{:keys [pos words]} state
                                   sym (first (get words pos))]
                               (when (symbol? sym)
                                 (get-docs sym)))))
                     submit (fn [text]
                              (if (= "clear" (.trim text))
                                (do
                                  (clear-items)
                                  (set-text ""))
                                (when (pos? (count (.trim text)))
                                  (set-text text)
                                  (add-input text)
                                  (execute text #(add-result (not %1) %2)))))]

    (set-print! add-log)
    [:<>
     (when @(rf/subscribe [::app.subs/panel-visible? :repl-history])
       [repl-items-panel @items show-value-opts set-text])

     [:div.relative.whitespace-pre-wrap.font-mono
      {:dir "ltr"}
      [completion-list
       @docs
       @complete-atom
       ;; TODO: this should also replace the text....
       identity
       #_(swap! complete-atom assoc :pos % :active true)]
      (let [_items @items] ; TODO: This needs to be removed
        [repl-input
         (reepl.subs/current-text state)
         submit
         {:complete-word complete-word
          :on-up go-up
          :on-down go-down
          :complete-atom complete-atom
          :on-change set-text
          :js-cm-opts js-cm-opts
          :on-cm-init on-cm-init}])]]))

(defonce state (reagent/atom db/initial-state))

(defn root
  []
  (let [repl-mode (rf/subscribe [::app.subs/repl-mode])
        debug-info (rf/subscribe [::app.subs/debug-info])
        codemirror-theme @(rf/subscribe [::theme.subs/codemirror])]
    [repl
     :execute #(reepl.replumb/run-repl (if (= @repl-mode :cljs)
                                         %1
                                         (str "(js/eval \"" %1 "\")"))
                                       {:verbose @debug-info} %2)
     :complete-word (fn [text] (reepl.replumb/process-apropos @repl-mode text))
     :get-docs reepl.replumb/process-doc
     :state state
     :show-value-opts
     {:showers [show-devtools/show-devtools
                (partial show-function/show-fn-with-docs maybe-fn-docs)]}
     :js-cm-opts {:mode (if (= @repl-mode :cljs) "clojure" "javascript")
                  :keyMap "default"
                  :showCursorWhenSelecting true
                  :theme codemirror-theme}]))
