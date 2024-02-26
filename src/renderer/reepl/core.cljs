(ns renderer.reepl.core
  (:require
   ["react" :as react]
   ["react-resizable-panels" :refer [Panel PanelResizeHandle]]
   [cljs.reader]
   [cljs.tools.reader]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [renderer.components :as comp]
   [renderer.reepl.codemirror :as code-mirror]
   [renderer.reepl.completions :refer [completion-list]]
   [renderer.reepl.handlers :as handlers]
   [renderer.reepl.repl-items :refer [repl-items]]
   [renderer.reepl.subs :as subs]
   [replumb.core :as replumb])
  (:require-macros
   [reagent.ratom :refer [reaction]]))

(defn is-valid-cljs?
  [source]
  (try
    (fn []
      (cljs.tools.reader/read-string source)
      true)
    (catch js/Error _
      false)))

;; TODO: these should probably go inside code-mirror.cljs? They are really
;; coupled to CodeMirror....
(def default-cm-opts
  {:should-go-up
   (fn [_source inst]
     (let [pos (.getCursor inst)]
       (= 0 (.-line pos))))

   :should-go-down
   (fn [_source inst]
     (let [pos (.getCursor inst)
           last-line (.lastLine inst)]
       (= last-line (.-line pos))))

   ;; TODO: if the cursor is inside a list, and the function doesn't have enought
   ;; arguments yet, then return false
   ;; e.g. (map |) <- map needs at least one argument.
   :should-eval
   (fn [source inst evt]
     (if (.-shiftKey evt)
       false
       (if (.-metaKey evt)
         true
         (let [lines (.lineCount inst)
               in-place (or (= 1 lines)
                            (let [pos (.getCursor inst)
                                  last-line (dec lines)]
                              (and
                               (= last-line (.-line pos))
                               (= (.-ch pos)
                                  (count (.getLine inst last-line))))))]
           (and in-place
                (is-valid-cljs? source))))))})

#_(defn repl-mode-button
    [mode]
    (let [repl-mode @(rf/subscribe [:repl-mode])
          active? (= repl-mode mode)]
      [:button.icon-button {:style {:class (when active? "overlay")
                                    :color (when active? "var(--font-color-active)")}
                            :on-click #(rf/dispatch [:set-repl-mode mode])}
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
        repl-history? @(rf/subscribe [:panel/visible? :repl-history])]
    [:div.toolbar {:style {:padding-top "0" :padding-bottom "0"}}
     [:div.flex.text-xs.pl-1 {:style {:margin-top "7px"}} (replumb/get-prompt)]
     ^{:key (str (hash (:js-cm-opts cm-opts)))}
     [code-mirror/code-mirror (reaction (:text @state))
      (merge
       default-cm-opts
       {:style {:height "auto"
                :flex 1
                :padding "2px 0"}
        :on-eval submit}
       cm-opts)]
     #_[:div
        (repl-mode-button :cljs)
        (repl-mode-button :js)]
     [comp/toggle-icon-button
      {:class "hover:bg-transparent my-0"
       :active? repl-history?
       :active-icon "chevron-down"
       :active-text "Hide command output"
       :inactive-icon "chevron-up"
       :inactive-text "Show command output"
       :action #(rf/dispatch [:panel/toggle :repl-history])}
      {:style {:margin-top "0" :margin-bottom "0"}}]]))

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

(def initial-state
  {:items []
   :hist-pos 0
   :history [""]})

;; TODO: is there a macro or something that could do this cleaner?
(defn make-handlers
  [state]
  {:add-input (partial swap! state handlers/add-input)
   :add-result (partial swap! state handlers/add-result)
   :go-up (partial swap! state handlers/go-up)
   :go-down (partial swap! state handlers/go-down)
   :clear-items (partial swap! state handlers/clear-items)
   :set-text (partial swap! state handlers/set-text)
   :add-log (partial swap! state handlers/add-log)})

(defn repl
  [& {:keys [execute
             _complete-word
             get-docs
             state
             _show-value-opts
             _js-cm-opts
             _on-cm-init]}]
  (let [state (or state (r/atom initial-state))
        {:keys
         [add-input
          add-result
          go-up
          go-down
          clear-items
          set-text
          add-log]} (make-handlers state)

        items (subs/items state)
        complete-atom (r/atom nil)
        docs (reaction
              (let [{:keys [pos list] :as state} @complete-atom]
                (when state
                  (let [sym (first (get list pos))]
                    (when (symbol? sym)
                      (get-docs sym))))))
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

    (fn [& {:keys [_execute
                   complete-word
                   _get-docs
                   state
                   show-value-opts
                   js-cm-opts
                   on-cm-init]}]
      [:<>
       (when @(rf/subscribe [:panel/visible? :repl-history])
         [:<>
          [:> PanelResizeHandle
           {:id "repl-resize-handle"
            :className "resize-handle"}]
          [:> Panel
           {:id "repl-panel"
            :minSize 10
            :defaultSize 10
            :order 3}
           [repl-items @items (assoc show-value-opts :set-text set-text)]]])

       [:div.relative.whitespace-pre-wrap.font-mono
        [completion-list
         @docs
         @complete-atom
        ;; TODO: this should also replace the text....
         identity
         #_(swap! complete-atom assoc :pos % :active? true)]
        (let [_items @items] ; TODO: This needs to be removed
          [repl-input
           (subs/current-text state)
           submit
           {:complete-word complete-word
            :on-up go-up
            :on-down go-down
            :complete-atom complete-atom
            :on-change set-text
            :js-cm-opts js-cm-opts
            :on-cm-init on-cm-init}])]])))
