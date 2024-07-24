(ns renderer.reepl.core
  (:require
   ["react-resizable-panels" :refer [Panel PanelResizeHandle]]
   [cljs.reader]
   [cljs.tools.reader]
   [platform]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.components :as comp]
   [renderer.reepl.codemirror :as code-mirror]
   [renderer.reepl.completions :refer [completion-list]]
   [renderer.reepl.handlers :as h]
   [renderer.reepl.repl-items :refer [repl-items]]
   [renderer.reepl.subs :as s]
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

(defn repl-mode-button
  [mode]
  (let [repl-mode @(rf/subscribe [:repl-mode])
        active? (= repl-mode mode)]
    [:button.button.rounded.px-1.leading-none.text-2xs.h-4
     {:class [(when active? "selected")
              "m-0.5"]
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
    [:div.flex.p-0.5.items-center.m-1
     [:div.flex.text-xs.self-start {:class "m-0.5"} (replumb/get-prompt)]
     ^{:key (str (hash (:js-cm-opts cm-opts)))}
     [code-mirror/code-mirror (reaction (:text @state))
      (merge
       default-cm-opts
       {:style {:height "auto"
                :flex 1}
        :on-eval submit}
       cm-opts)]
     [:div.self-start.h-full.flex.items-center
      (repl-mode-button :cljs)
      (repl-mode-button :js)
      (when platform/electron? (repl-mode-button :py))]
     [comp/toggle-icon-button
      {:active? repl-history?
       :active-icon "chevron-down"
       :active-text "Hide command output"
       :inactive-icon "chevron-up"
       :inactive-text "Show command output"
       :action #(rf/dispatch [:panel/toggle :repl-history])}
      {:style {:height "16px"}}]]))

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
  {:add-input (partial swap! state h/add-input)
   :add-result (partial swap! state h/add-result)
   :go-up (partial swap! state h/go-up)
   :go-down (partial swap! state h/go-down)
   :clear-items (partial swap! state h/clear-items)
   :set-text (partial swap! state h/set-text)
   :add-log (partial swap! state h/add-log)})

(defn repl-items-panel
  [items show-value-opts set-text]
  [:<>
   [:> PanelResizeHandle
    {:id "repl-resize-handle"
     :className "resize-handle"}]
   [:> Panel
    {:id "repl-panel"
     :minSize 10
     :defaultSize 10
     :order 3}
    [repl-items items (assoc show-value-opts :set-text set-text)]]])

(defn repl
  [& {:keys [execute
             complete-word
             get-docs
             state
             show-value-opts
             js-cm-opts
             on-cm-init]}]
  (ra/with-let [state (or state (ra/atom initial-state))
                {:keys [add-input
                        add-result
                        go-up
                        go-down
                        clear-items
                        set-text
                        add-log]} (make-handlers state)
                items (s/items state)
                complete-atom (ra/atom nil)
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
    [:<>
     (when @(rf/subscribe [:panel/visible? :repl-history])
       [repl-items-panel @items show-value-opts set-text])

     [:div.relative.whitespace-pre-wrap.font-mono
      [completion-list
       @docs
       @complete-atom
        ;; TODO: this should also replace the text....
       identity
       #_(swap! complete-atom assoc :pos % :active? true)]
      (let [_items @items] ; TODO: This needs to be removed
        [repl-input
         (s/current-text state)
         submit
         {:complete-word complete-word
          :on-up go-up
          :on-down go-down
          :complete-atom complete-atom
          :on-change set-text
          :js-cm-opts js-cm-opts
          :on-cm-init on-cm-init}])]]))
