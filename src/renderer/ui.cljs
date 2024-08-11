(ns renderer.ui
  "A collection of stateless reusable ui components.
   Avoid using subscriptions here as much as possible."
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-switch" :as Switch]
   ["react-svg" :refer [ReactSVG]]
   [re-frame.core :as rf]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.utils.keyboard :as keyb]))

(defn icon
  [icon-name attrs]
  [:> ReactSVG
   (merge {:class "icon"
           :src (str "icons/" icon-name ".svg")}
          attrs)])

(defn icon-button
  [icon-name props]
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
  [[shortcut]] ; REVIEW
  (->> (cond-> []
         (:ctrlKey shortcut) (conj "Ctrl")
         (:shiftKey shortcut) (conj "⇧")
         (:altKey shortcut) (conj "Alt")
         :always (conj (keyb/code->key (:keyCode shortcut))))
       (map #(into [:span.shortcut-key] %))
       (interpose [:span {:class "px-0.5"} "+"])
       (into [:span])))

(defn shortcuts
  [event]
  (let [event-shortcuts @(rf/subscribe [:event-shortcuts event])]
    (when (seq event-shortcuts)
      (->> event-shortcuts
           (map format-shortcut)
           (interpose [:span])
           (into [:span.inline-flex.text-muted {:class "gap-1.5"}])))))

(defn toggle-icon-button
  [{:keys [active? active-icon inactive-icon active-text inactive-text action class]}
   attrs]
  [:button.icon-button
   (merge attrs
          {:class class
           :title (if active? active-text inactive-text)
           :on-double-click #(.stopPropagation %)
           :on-pointer-up #(when action
                             (.stopPropagation %)
                             (action))})
   [icon (if active? active-icon inactive-icon)]])

(defn toggle-collapsed-button
  [k collapsed?]
  [toggle-icon-button {:active? collapsed?
                       :active-icon "chevron-right"
                       :active-text "expand"
                       :class "small"
                       :inactive-icon "chevron-down"
                       :inactive-text "collapse"
                       :action #(rf/dispatch (if collapsed?
                                               [::document.e/expand-el k]
                                               [::document.e/collapse-el k]))}])


(defn radio-icon-button
  [{:keys [active? icon title action class]}]
  [:button.icon-button.radio-icon-button
   {:title title
    :class [class (when active? "selected")]
    :on-click action}
   [renderer.ui/icon icon]])

(defn context-menu-item
  [{:keys [type label action checked? disabled?]}]
  (case type
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
  [{:keys [type label action checked?]}]
  (case type
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
