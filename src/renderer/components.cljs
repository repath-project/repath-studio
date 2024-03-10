(ns renderer.components
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-switch" :as Switch]
   ["react-svg" :refer [ReactSVG]]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.utils.keyboard :as keyb]))

(defn icon
  [icon attrs]
  [:> ReactSVG
   (merge {:class "icon"
           :src (str "icons/" icon ".svg")}
          attrs)])

(defn icon-button
  [icon props]
  [:button.icon-button
   props
   [renderer.components/icon icon]])

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
       (str/join "+")))

(defn shortcuts
  [event]
  (let [shortcuts @(rf/subscribe [:event-shortcuts event])]
    (when (seq shortcuts)
      (->> shortcuts
           (map format-shortcut)
           (interpose [:span.text-muted " | "])
           (into [:span.shortcut])))))

(defn toggle-icon-button
  [{:keys [active? active-icon inactive-icon active-text inactive-text action class]}
   attrs]
  [:button.icon-button
   (merge attrs
          {:class class
           :title (if active? active-text inactive-text)
           :on-double-click #(.stopPropagation %)
           :on-click #(when action
                        (.stopPropagation %)
                        (action))})
   [icon (if active? active-icon inactive-icon)]])

(defn toggle-collapsed-button
  [el-k collapsed?]
  [toggle-icon-button {:active? collapsed?
                       :active-icon "chevron-right"
                       :active-text "expand"
                       :class "small"
                       :inactive-icon "chevron-down"
                       :inactive-text "collapse"
                       :action #(rf/dispatch (if collapsed?
                                               [:document/expand-el el-k]
                                               [:document/collapse-el el-k]))}])


(defn radio-icon-button
  [{:keys [active? icon title action class]}]
  [:button.icon-button.radio-icon-button
   {:title title
    :class [class (when active? "selected")]
    :on-click action}
   [renderer.components/icon icon]])

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
      :onSelect #(rf/dispatch action)
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

(def element-menu
  ;; TODO: Add and group actions
  [{:label "Cut"
    :action [:element/cut]}
   {:label "Copy"
    :action [:element/copy]}
   {:label "Paste"
    :action [:element/paste]}
   {:type :separator}
   {:label "Raise"
    :action [:element/raise]}
   {:label "Lower"
    :action [:element/lower]}
   {:label "Raise to top"
    :action [:element/raise-to-top]}
   {:label "Lower to bottom"
    :action [:element/lower-to-bottom]}
   {:type :separator}
   {:label "Animate"
    :action [:element/animate :animate {}]}
   {:label "Animate Transform"
    :action [:element/animate :animateTransform {}]}
   {:label "Animate Motion"
    :action [:element/animate :animateMotion {}]}
   {:type :separator}
   {:label "Duplicate in position"
    :action [:element/duplicate-in-place]}
   {:label "Delete"
    :action [:element/delete]}])
