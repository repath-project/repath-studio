(ns renderer.ui
  "A collection of stateless reusable ui components.
   Avoid using subscriptions here as much as possible."
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-scroll-area" :as ScrollArea]
   ["@radix-ui/react-switch" :as Switch]
   ["react-svg" :refer [ReactSVG]]
   [malli.experimental :as mx]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.utils.hiccup :refer [Props]]
   [renderer.utils.keyboard :as keyb]))

(mx/defn icon
  [icon-name :- string?, props :- Props]
  [:> ReactSVG
   (merge props {:class ["icon" (:class props)]
                 :src (str "icons/" icon-name ".svg")})])

(mx/defn icon-button
  [icon-name :- string?, props :- Props]
  [:button.icon-button
   props
   [icon icon-name]])

(defn switch
  [{:keys [id label default-checked? on-checked-change]}]
  [:span.inline-flex.items-center
   [:label.switch-label {:for id} label]
   [:> Switch/Root
    {:class "switch-root"
     :id id
     :default-checked default-checked?
     :on-checked-change on-checked-change}
    [:> Switch/Thumb {:class "switch-thumb"}]]])

(defn format-shortcut
  [[shortcut]]
  (->> (cond-> []
         (:ctrlKey shortcut) (conj "Ctrl")
         (:shiftKey shortcut) (conj "⇧")
         (:altKey shortcut) (conj "Alt")
         :always (conj (keyb/key-code->key (:keyCode shortcut))))
       (map #(into [:span.shortcut-key] %))
       (interpose [:span {:class "px-0.5"} "+"])
       (into [:span])))

(mx/defn shortcuts
  [event :- vector?]
  (let [event-shortcuts @(rf/subscribe [::app.s/event-shortcuts event])]
    (when (seq event-shortcuts)
      (->> event-shortcuts
           (map format-shortcut)
           (interpose [:span])
           (into [:span.inline-flex.text-muted {:class "gap-1.5"}])))))

(mx/defn radio-icon-button
  [icon-name :- string?, active :- boolean?, & {:keys [class] :as props} :- Props]
  [:button.icon-button.radio-icon-button
   (assoc props :class [class (when active "selected")])
   [renderer.ui/icon icon-name]])

(defn context-menu-item
  [{:keys [label action checked? disabled?] :as attrs}]
  (case (:type attrs)
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
     label
     [:div.right-slot
      [shortcuts action]]]

    [:> ContextMenu/Item
     {:class "menu-item context-menu-item"
      :onSelect #(rf/dispatch action)
      :disabled disabled?}
     label
     [:div.right-slot
      [shortcuts action]]]))

(defn dropdown-menu-item
  [{:keys [label action checked?] :as attrs}]
  (case (:type attrs)
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
     label
     [:div.right-slot
      [shortcuts action]]]

    [:> DropdownMenu/Item
     {:class "menu-item dropdown-menu-item"
      :onSelect #(rf/dispatch action)}
     label
     [:div.right-slot
      [shortcuts action]]]))

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
      {:class "flex touch-none p-0.5 select-none hover:overlay w-2.5"
       :orientation "vertical"}
      [:> ScrollArea/Thumb
       {:class "relative flex-1 overlay rounded-full"}]]

     [:> ScrollArea/Scrollbar
      {:class "flex touch-none p-0.5 select-none hover:overlay flex-col h-2.5"
       :orientation "horizontal"}
      [:> ScrollArea/Thumb
       {:class "relative flex-1 overlay rounded-full"}]]

     [:> ScrollArea/Corner]]))
