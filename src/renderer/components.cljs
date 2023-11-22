(ns renderer.components
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["react-svg" :refer [ReactSVG]]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.utils.keyboard :as keyboard]))

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
  [k direction]
  [:div.resizer
   [:div.resize-handler
    {:draggable true
     :on-drag-over #(.preventDefault %)
     :on-drag-start (fn [e]
                      (.setDragImage (.-dataTransfer e)
                                     (.createElement js/document "img")
                                     0 0)
                      (rf/dispatch-sync [:panel/set-drag k direction]))
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
     [:div.right-slot
      [shortcuts action]]]

    [:> ContextMenu/Item
     {:class "menu-item context-menu-item"
      :onSelect #(rf/dispatch action)}
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
