(ns renderer.views
  "A collection of stateless reusable ui components.
   Avoid using subscriptions to keep the components pure."
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-popover" :as Popover]
   ["@radix-ui/react-scroll-area" :as ScrollArea]
   ["@radix-ui/react-slider" :as Slider]
   ["@radix-ui/react-switch" :as Switch]
   ["@repath-project/react-color" :refer [PhotoshopPicker]]
   ["codemirror" :as codemirror]
   ["codemirror/addon/display/placeholder.js"]
   ["codemirror/addon/hint/css-hint.js"]
   ["codemirror/addon/hint/show-hint.js"]
   ["codemirror/mode/css/css.js"]
   ["codemirror/mode/xml/xml.js"]
   ["react" :as react]
   ["react-svg" :refer [ReactSVG]]
   ["tailwind-merge" :refer [twMerge]]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.subs :as-alias app.subs]
   [renderer.event.impl.keyboard :as event.impl.keyboard]))

(defn merge-with-class
  [& maps]
  (-> (apply merge maps)
      (assoc :class (apply twMerge (map :class maps)))))

(defn icon
  [icon-name props]
  [:> ReactSVG
   (merge-with-class {:class "icon"
                      :src (str "icons/" icon-name ".svg")} props)])

(defn icon-button
  [icon-name props]
  [:button.icon-button props [icon icon-name]])

(defn loading-indicator
  []
  [icon "spinner" {:class "animate-spin"}])

(defn switch
  [label props]
  [:span.inline-flex.items-center
   [:label.form-element.h-auto.bg-transparent
    {:for (:id props)}
    label]
   [:> Switch/Root
    (merge-with-class
     {:class "overlay relative rounded-full w-10 h-6
              data-[state=checked]:bg-accent data-disabled:opacity-50"
      :dir "ltr"}
     props)
    [:> Switch/Thumb
     {:class "block bg-primary rounded-full shadow-sm w-5 h-5
              will-change-transform transition-transform translate-x-0.5
              data-[state=checked]:translate-x-[18px]"}]]])

(defn slider
  [props]
  [:> Slider/Root
   (merge-with-class
    {:class "relative flex items-center select-none w-full touch-none h-full"}
    props)
   [:> Slider/Track {:class "relative h-1 bg-secondary flex-1"}
    [:> Slider/Range {:class "absolute h-full overlay-2x"}]]
   [:> Slider/Thumb {:class "slider-thumb"
                     :aria-label "Resize panel thumb"}]])

(defn format-shortcut
  [[shortcut]]
  (into [:div.flex.gap-1.items-center {:dir "ltr"}]
        (comp (map (partial into [:span.shortcut-key]))
              (interpose [:span "+"]))
        (-> (cond-> []
              (:ctrlKey shortcut) (conj "Ctrl")
              (:shiftKey shortcut) (conj "⇧")
              (:altKey shortcut) (conj "Alt"))

            (conj (event.impl.keyboard/key-code->key (:keyCode shortcut))))))

(defn shortcuts
  [event]
  (let [event-shortcuts @(rf/subscribe [::app.subs/event-shortcuts event])]
    (when (seq event-shortcuts)
      (into [:span.inline-flex.text-muted {:class "gap-1.5"}]
            (comp (map format-shortcut)
                  (interpose [:span]))
            event-shortcuts))))

(defn radio-icon-button
  [icon-name active props]
  [:button.icon-button
   (merge-with-class {:class (str (when active "accent ") "active:overlay")} props)
   [renderer.views/icon icon-name]])

(defn context-menu-item
  [{:keys [label action checked? disabled?] :as props}]
  (case (:type props)
    :separator
    [:> ContextMenu/Separator {:class "menu-separator"}]

    :checkbox
    [:> ContextMenu/CheckboxItem
     {:class "menu-checkbox-item inset"
      :onSelect #(rf/dispatch action)
      :checked @(rf/subscribe checked?)
      :disabled disabled?}
     [:> ContextMenu/ItemIndicator
      {:class "menu-item-indicator"}
      [icon "checkmark"]]
     [:div label]
     [shortcuts action]]

    [:> ContextMenu/Item
     {:class "menu-item context-menu-item"
      :onSelect #(rf/dispatch action)
      :disabled disabled?}
     [:div label]
     [shortcuts action]]))

(defn dropdown-menu-item
  [{:keys [label action checked?] :as props}]
  (case (:type props)
    :separator
    [:> DropdownMenu/Separator {:class "menu-separator"}]

    :checkbox
    [:> DropdownMenu/CheckboxItem
     {:class "menu-checkbox-item inset"
      :onSelect #(do (.preventDefault %)
                     (rf/dispatch action))
      :checked @(rf/subscribe checked?)}
     [:> DropdownMenu/ItemIndicator
      {:class "menu-item-indicator"}
      [icon "checkmark"]]
     [:div label]
     [shortcuts action]]

    [:> DropdownMenu/Item
     {:class "menu-item dropdown-menu-item"
      :onSelect #(rf/dispatch action)}
     [:div label]
     [shortcuts action]]))

(defn scroll-area
  [& more]
  (let [children (if (map? (first more)) (rest more) more)]
    [:> ScrollArea/Root
     {:class "overflow-hidden w-full"}
     (into
      [:> ScrollArea/Viewport
       {:ref (:ref (first more))
        :class "w-full h-full"}] children)

     [:> ScrollArea/Scrollbar
      {:class "flex touch-none p-0.5 select-none w-2.5"
       :orientation "vertical"}
      [:> ScrollArea/Thumb
       {:class "relative flex-1 overlay rounded-full"}]]

     [:> ScrollArea/Scrollbar
      {:class "flex touch-none p-0.5 select-none flex-col h-2.5"
       :orientation "horizontal"}
      [:> ScrollArea/Thumb
       {:class "relative flex-1 overlay hover:overlay-2x rounded-full"}]]

     [:> ScrollArea/Corner]]))

(defn color-picker
  [props & children]
  [:> Popover/Root {:modal true}
   (into [:> Popover/Trigger {:as-child true}]
         children)
   [:> Popover/Portal
    [:> Popover/Content
     {:class "popover-content max-w-fit"
      :align "start"
      :side "top"
      :align-offset (:align-offset props)}
     [:div {:dir "ltr"}
      [:> PhotoshopPicker props]]
     [:> Popover/Arrow {:class "popover-arrow"}]]]])

(def cm-defaults
  {:lineNumbers false
   :matchBrackets true
   :lineWrapping true
   :styleActiveLine true
   :tabMode "spaces"
   :autofocus false
   :extraKeys {"Ctrl-Space" "autocomplete"}
   :theme "tomorrow-night-eighties"
   :autoCloseBrackets true})

(defn cm-render-line
  "Line up wrapped text with the base indentation.
   https://codemirror.net/demo/indentwrap.html"
  [editor line el]
  (let [tab-size (.getOption editor "tabSize")
        off (* (.countColumn codemirror (.-text line) nil tab-size)
               (.defaultCharWidth editor))]
    (set! (.. el -style -textIndent)
          (str "-" off "px"))
    (set! (.. el -style -paddingLeft)
          (str (+ 4 off) "px"))))

(defn cm-editor
  [value {:keys [attrs options on-init on-blur]}]
  (let [cm (reagent/atom nil)
        ref (react/createRef)]
    (reagent/create-class
     {:component-did-mount
      (fn [_this]
        (let [el (.-current ref)
              options (clj->js (merge cm-defaults options))]
          (reset! cm (.fromTextArea codemirror el options))
          (.setValue @cm value)
          (.on @cm "renderLine" cm-render-line)
          (.on @cm "keydown" (fn [_editor evt] (.stopPropagation evt)))
          (.on @cm "keyup" (fn [_editor evt] (.stopPropagation evt)))
          (.refresh @cm)
          (when on-blur (.on @cm "blur" #(on-blur (.getValue %))))
          (when on-init (on-init @cm))))

      :component-will-unmount
      #(when @cm (reset! cm nil))

      :component-did-update
      (fn [this _]
        (let [value (second (reagent/argv this))
              options (:options (last (reagent/argv this)))]
          (.setValue @cm value)
          (doseq [[k v] options]
            (.setOption @cm (name k) v))))

      :reagent-render
      (fn [value]
        [:textarea (merge {:value value
                           :on-blur #()
                           :on-change #()
                           :ref ref} attrs)])})))
