(ns renderer.reepl.core
  (:require
   [cljs.reader]
   [cljs.tools.reader]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [renderer.components :as comp]
   [renderer.reepl.codemirror :as code-mirror]
   [renderer.reepl.completions :refer [completion-list]]
   [renderer.reepl.handlers :as handlers]
   [renderer.reepl.helpers :as helpers]
   [renderer.reepl.repl-items :refer [repl-items]]
   [renderer.reepl.subs :as subs]
   [replumb.core :as replumb])
  (:require-macros
   [reagent.ratom :refer [reaction]]))

(def styles
  {:container {:font-family "var(--font-mono)"
               :flex 1
               :display :flex
               :white-space "pre-wrap"
               :flex-wrap "wrap"
               :position :relative}

   :intro-message {:padding "10px 20px"
                   :line-height 1.5
                   :border-bottom "1px solid #aaa"
                   :flex-direction :row
                   :margin-bottom 10}
   :intro-code {:background-color "#eee"
                :padding "0 5px"}

   :completion-container {:font-size 12}
   :completion-list {:flex-direction :row
                     :overflow :hidden
                     :height 20}
   :completion-empty {:color "#ccc"
                      ;;:font-weight :bold
                      :padding "3px 10px"}

   :completion-show-all {:position :absolute
                         :top 0
                         :left 0
                         :right 0
                         :z-index 1000
                         :flex-direction :row
                         :background-color "#eef"
                         :flex-wrap :wrap}
   :completion-item {;; :cursor :pointer TODO make these clickable
                     :padding "3px 5px 3px"}
   :completion-selected {:background-color "#eee"}
   :completion-active {:background-color "#aaa"}

   :docs {:max-height 300
          :overflow :auto
          :padding "16px"
          :position "absolute"
          :bottom "100%"
          :margin-bottom "24px"
          :background "var(--level-3)"
          :left 0
          :font-size "13px"
          :box-shadow "0 0 15px rgba(0, 0, 0, .25)"}

   :main-caret {:padding "8px 5px 8px 10px"
                :margin-right 0
                :flex-direction :row}

   :input-caret {:color "#55f"
                 :margin-right 10}})

(def view (partial helpers/view styles))

(defn is-valid-cljs? [source]
  (try
    (fn []
      (cljs.tools.reader/read-string source)
      true)
    (catch js/Error _
      false)))

;; TODO these should probably go inside code-mirror.cljs? They are really
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

   ;; TODO if the cursor is inside a list, and the function doesn't have enought
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

(defn repl-input [state submit repl-history? cm-opts]
  {:pre [(every? (comp not nil?)
                 (map cm-opts
                      [:on-up
                       :on-down
                       :complete-atom
                       :complete-word
                       :on-change]))]}
  (let [{:keys [_pos _count _text]} @state]
    [:div.flex
     [:div.flex.pl-2 {:style {:font-size "12px"
                              :margin-top "6px"}} (replumb/get-prompt)]
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
     [:div.toolbar
      [comp/toggle-icon-button
       {:class "small"
        :active? (not repl-history?)
        :active-icon "chevron-up"
        :active-text "show command output"
        :inactive-icon "chevron-down"
        :inactive-text "hide command output"
        :action #(rf/dispatch [:panel/toggle :repl-history])}]]]))

(defn docs-view [docs]
  (when docs [view :docs docs]))

(defn set-print! [log]
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

;; TODO is there a macro or something that could do this cleaner?
(defn make-handlers [state]
  {:add-input (partial swap! state handlers/add-input)
   :add-result (partial swap! state handlers/add-result)
   :go-up (partial swap! state handlers/go-up)
   :go-down (partial swap! state handlers/go-down)
   :clear-items (partial swap! state handlers/clear-items)
   :set-text (partial swap! state handlers/set-text)
   :add-log (partial swap! state handlers/add-log)})

(defn repl [& {:keys [execute
                      _complete-word
                      get-docs
                      state
                      _show-value-opts
                      _js-cm-opts
                      _on-cm-init]}]
  (let [repl-history? (rf/subscribe [:panel/visible? :repl-history])
        state (or state (r/atom initial-state))
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
                   (when (< 0 (count (.trim text)))
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
      [:div
       (when @repl-history? [repl-items @items (assoc show-value-opts :set-text set-text)])
       [view :container
        (when @docs [docs-view @docs])
        [completion-list
         @complete-atom
        ;; TODO this should also replace the text....
         identity
         #_(swap! complete-atom assoc :pos % :active true)]
        (let [_items @items] ; TODO This needs to be removed
          [repl-input
           (subs/current-text state)
           submit
           @repl-history?
           {:complete-word complete-word
            :on-up go-up
            :on-down go-down
            :complete-atom complete-atom
            :on-change set-text
            :js-cm-opts js-cm-opts
            :on-cm-init on-cm-init}])]])))
