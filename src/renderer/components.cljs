(ns renderer.components
  (:require
   [re-frame.core :as rf]
   [renderer.utils.keyboard :as keyboard]
   [clojure.string :as str]
   ["react-svg" :refer [ReactSVG]]
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]))

(defn icon
  [icon {:keys [class]}]
  [:> ReactSVG {:class ["icon" class] :src (str "icons/" icon ".svg")}])

(defn icon-button
  [icon props]
  [:button.icon-button
   props
   [renderer.components/icon icon]])

(defn shortcuts
  [event]
  (let [shortcuts @(rf/subscribe [:event-shortcuts event])]
    (into [:div.shortcuts]
          (interpose [:span.text-muted " | "]
                     (map (fn [[shortcut]]
                            (str/join "+"
                                      (cond-> []
                                        (:ctrlKey shortcut)
                                        (conj "Ctrl")

                                        (:shiftKey shortcut)
                                        (conj "⇧")

                                        (:altKey shortcut)
                                        (conj "Alt")

                                        :always (conj (keyboard/code->key
                                                       (:keyCode shortcut))))))
                          shortcuts)))))

(defn toggle-icon-button
  [{:keys [active?
           active-icon
           inactive-icon
           active-text
           inactive-text
           action
           class]}]
  [:button.icon-button {:class class
                        :title (if active? active-text inactive-text)
                        :on-click #(when action
                                     (.stopPropagation %)
                                     (action))}
   [icon (if active? active-icon inactive-icon) {:fixed-width true}]])

(defn toggle-collapsed-button
  [collapsed? action]
  [toggle-icon-button {:active? collapsed?
                       :active-icon "chevron-right"
                       :active-text "expand"
                       :class "small"
                       :inactive-icon "chevron-down"
                       :inactive-text "collapse"
                       :action action}])


(defn radio-icon-button
  [{:keys [active? icon title action class]}]
  [:button.icon-button.radio-icon-button
   {:title title
    :class [class (when active? "selected")]
    :on-click action}
   [renderer.components/icon icon]])

(defn resizer
  [key direction]
  [:div.resizer
   [:div.resize-handler
    {:draggable true
     :on-drag-over #(.preventDefault %)
     :on-drag-start #(do (.setDragImage (.-dataTransfer %)
                                        (.createElement js/document "img")
                                        0 0)
                         (rf/dispatch-sync [:panel/set-drag key direction]))
     :on-drag-end #(rf/dispatch-sync [:panel/clear-drag])}]])

(defn context-menu-item
  [{:keys [type label action checked?]}]
  (case type
    :separator
    [:> ContextMenu/Separator {:class "menu-separator"}]

    :checkbox
    [:> ContextMenu/CheckboxItem
     {:class "menu-checkbox-item inset"
      :onSelect #(rf/dispatch action)
      :checked @(rf/subscribe checked?)}
     [:> ContextMenu/ItemIndicator
      {:class "menu-item-indicator"}
      [icon "checkmark"]]
     label
     [:div {:class "right-slot"}
      [shortcuts action]]]

    [:> ContextMenu/Item
     {:class "menu-item context-menu-item"
      :onSelect #(rf/dispatch action)}
     label
     [:div {:class "right-slot"}
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
     [:div {:class "right-slot"}
      [shortcuts action]]]

    [:> DropdownMenu/Item
     {:class "menu-item dropdown-menu-item"
      :onSelect #(rf/dispatch action)}
     label
     [:div {:class "right-slot"}
      [shortcuts action]]]))

(def element-menu
  [{:label "Cut"
    :action [:elements/cut]}
   {:label "Copy"
    :action [:elements/copy]}
   {:label "Paste"
    :action [:elements/paste]}
   {:type :separator}
   {:label "Raise"
    :action [:elements/raise]}
   {:label "Lower"
    :action [:elements/lower]}
   {:label "Raise to top"
    :action [:elements/raise-to-top]}
   {:label "Lower to bottom"
    :action [:elements/lower-to-bottom]}
   {:type :separator}
   {:label "Animate"
    :action [:elements/animate :animate {}]}
   {:label "Animate Transform"
    :action [:elements/animate :animateTransform {}]}
   {:label "Animate Motion"
    :action [:elements/animate :animateMotion {}]}
   {:type :separator}
   {:label "Duplicate in position"
    :action [:elements/duplicate-in-place]}
   {:label "Delete"
    :action [:elements/delete]}])