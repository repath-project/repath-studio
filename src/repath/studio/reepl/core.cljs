(ns repath.studio.reepl.core
  (:require [cljs.reader]
            [cljs.tools.reader]
            [reagent.core :as r]
            [repath.studio.reepl.codemirror :as code-mirror]
            [repath.studio.reepl.repl-items :refer [repl-items]]
            [repath.studio.reepl.completions :refer [completion-list]]
            [repath.studio.reepl.handlers :as handlers]
            [repath.studio.reepl.subs :as subs]
            [repath.studio.reepl.helpers :as helpers]
            [repath.studio.styles :as styles]
            [repath.studio.components :as comp]
            [replumb.core :as replumb]
            [re-frame.core :as rf])
  (:require-macros
   [reagent.ratom :refer [reaction]]))

(def styles
  {:container {:font-family "Source Code Pro, monospace"
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
          :margin-bottom "21px"
          :background styles/level-3
          :left 0
          :font-size "13px"
          :box-shadow "0 0 15px rgba(0, 0, 0, .25)"}
   
   :main-caret {:padding "8px 5px 8px 10px"
                :margin-right 0
                :flex-direction :row}

   :input-caret {:color "#55f"
                 :margin-right 10}})

(def view (partial helpers/view styles))
(def text (partial helpers/text styles))
(def button (partial helpers/button styles))

(defn is-valid-cljs? [source]
  (try
    (do
      (cljs.tools.reader/read-string source)
      true)
    (catch js/Error _
      false)))

;; TODO these should probably go inside code-mirror.cljs? They are really
;; coupled to CodeMirror....
(def default-cm-opts
  {:should-go-up
   (fn [source inst]
     (let [pos (.getCursor inst)]
       (= 0 (.-line pos))))

   :should-go-down
   (fn [source inst]
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

(defn repl-input [state submit repl-history-collapsed? cm-opts]
  {:pre [(every? (comp not nil?)
                 (map cm-opts
                      [:on-up
                       :on-down
                       :complete-atom
                       :complete-word
                       :on-change]))]}
  (let [{:keys [pos count text]} @state]
    [:div.h-box
     [:span {:style {:padding "8px 0 8px 8px" :font-size "12px" :line-height "18px"}} (replumb/get-prompt)]
     ^{:key (str (hash (:js-cm-opts cm-opts)))}
     [code-mirror/code-mirror (reaction (:text @state))
      (merge
       default-cm-opts
       {:style {:height "auto"
                :flex 1
                :padding "4px 0"
                :font-size "12px"}
        :on-eval submit}
       cm-opts)]
     [comp/toggle-icon-button {:active? repl-history-collapsed?
                               :active-icon "chevron-up"
                               :active-text "show output"
                               :inactive-icon "chevron-down"
                               :inactive-text "hide output"
                               :action #(rf/dispatch [:toggle-repl-history-collapsed])}]]))

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
                      complete-word
                      get-docs
                      state
                      show-value-opts
                      js-cm-opts
                      on-cm-init]}]
  (let [repl-history-collapsed? (rf/subscribe [:repl-history-collapsed?])
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

    (fn [& {:keys [execute
                   complete-word
                   get-docs
                   state
                   show-value-opts
                   js-cm-opts
                   on-cm-init]}]
     [:div
      (when-not @repl-history-collapsed? [repl-items @items (assoc show-value-opts :set-text set-text)])
      [view :container
       (when @docs [docs-view @docs])
       [completion-list
        @complete-atom
        ;; TODO this should also replace the text....
        identity
        #_(swap! complete-atom assoc :pos % :active true)]
       (let [items @items] ; TODO This needs to be removed
         [repl-input
          (subs/current-text state)
          submit
          @repl-history-collapsed?
          {:complete-word complete-word
           :on-up go-up
           :on-down go-down
           :complete-atom complete-atom
           :on-change set-text
           :js-cm-opts js-cm-opts
           :on-cm-init on-cm-init}])]])))