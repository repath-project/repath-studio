(ns renderer.tree.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.components :as comp]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.events :as-alias element.e]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.events :as-alias frame.e]
   [renderer.tree.events :as-alias e]
   [renderer.utils.dom :as dom]
   [renderer.utils.keyboard :as keyb]))

(defn item-buttons
  [{:keys [key locked? visible?]}]
  [:<>
   [comp/toggle-icon-button
    {:active? visible?
     :active-icon "eye"
     :active-text "hide"
     :inactive-icon "eye-closed"
     :inactive-text "show"
     :class (when visible? "list-item-action")
     :action #(rf/dispatch [::element.e/toggle-prop key :visible?])}]
   [comp/toggle-icon-button
    {:active? locked?
     :active-icon "lock"
     :active-text "unlock"
     :inactive-icon "unlock"
     :inactive-text "lock"
     :class (when-not locked? "list-item-action")
     :action #(rf/dispatch [::element.e/toggle-prop key :locked?])}]])

(defn- set-item-name
  [e k]
  (rf/dispatch [::element.e/set-prop k :name (.. e -target -value)]))

(defn label
  [{:keys [key name visible? tag]}]
  (ra/with-let [edit-mode? (ra/atom false)]
    (if @edit-mode?
      [:input.list-item-input
       {:default-value name
        :placeholder tag
        :auto-focus true
        :on-key-down #(keyb/input-key-down-handler % name set-item-name key)
        :on-blur (fn [e]
                   (reset! edit-mode? false)
                   (set-item-name e key))}]
      [:div.flex
       [:div.truncate
        {:class [(when-not visible? "opacity-60")
                 (when (= :svg tag) "font-bold")]
         :style {:cursor "text"}
         :on-double-click (fn [e]
                            (.stopPropagation e)
                            (reset! edit-mode? true))}
        (if (empty? name) tag name)]])))

(defn drop-handler
  [e k]
  (let [parent-key (-> (.-dataTransfer e)
                       (.getData "key")
                       keyword)]
    (.preventDefault e)
    (rf/dispatch [::element.e/set-parent parent-key k])))

(defn padding
  [depth children?]
  (let [collapse-button-width 22]
    (- (* depth collapse-button-width)
       (if children? collapse-button-width 0))))

(defn list-item-button
  [{:keys [key selected? children] :as el} depth hovered? collapsed?]
  (let [multiple-selected? @(rf/subscribe [::element.s/multiple-selected?])]
    [:div.button.list-item-button
     {:class [(when selected? "selected")
              (when hovered? "hovered")]
      :tab-index 0
      :data-id key
      :role "menuitem"
      :on-double-click #(rf/dispatch [::frame.e/pan-to-element key])
      :on-pointer-enter #(rf/dispatch [::document.e/set-hovered-keys #{key}])
      :ref (fn [this]
             (when (and this selected? hovered? (not multiple-selected?))
               (dom/scroll-into-view! this)))
      :on-key-down #(do (.stopPropagation %)
                        (rf/dispatch [::e/key-down (.-key %) key (.-ctrlKey %)]))
      :draggable true
      :on-drag-start #(-> (.-dataTransfer %)
                          (.setData "key" (name key)))
      :on-drag-enter #(rf/dispatch [::document.e/set-hovered-keys #{key}])
      :on-drag-over #(.preventDefault %)
      :on-drop #(drop-handler % key)
      :on-pointer-down #(when (= (.-button %) 2)
                          (rf/dispatch [::element.e/select key (.-ctrlKey %)]))
      :on-pointer-up (fn [e]
                       (.stopPropagation e)
                       (rf/dispatch [::element.e/select key (.-ctrlKey e)]))
      :style {:padding-left (padding depth (seq children))}}
     [:div.flex.items-center.content-between.w-full
      (when (seq children)
        [comp/toggle-collapsed-button key collapsed?])
      [:div.flex-1.overflow-hidden [label el]]
      [item-buttons el]]]))

(defn item [{:keys [selected? children key] :as el} depth elements hovered-keys collapsed-keys]
  (let [has-children? (seq children)
        collapsed? (contains? collapsed-keys key)
        hovered? (contains? hovered-keys key)]
    [:li {:class (when selected? "overlay")}
     [list-item-button el depth hovered? collapsed?]
     (when (and has-children? (not collapsed?))
       [:ul (map (fn [el] [item el (inc depth) elements hovered-keys collapsed-keys])
                 (mapv (fn [key] (get elements key)) (reverse children)))])]))

(defn inner-sidebar-render
  [canvas-children elements]
  (let [hovered-keys @(rf/subscribe [::document.s/hovered-keys])
        collapsed-keys @(rf/subscribe [::document.s/collapsed-keys])]
    [:div.tree-sidebar.overflow-hidden
     {:on-pointer-up #(rf/dispatch [::element.e/deselect-all])}
     [:div.v-scroll.h-full
      [:ul
       {:on-pointer-leave #(rf/dispatch [::document.e/set-hovered-keys #{}])}
       (map (fn [el] [item el 1 elements hovered-keys collapsed-keys])
            (reverse canvas-children))]]]))

(defn inner-sidebar []
  (let [state @(rf/subscribe [:state])
        canvas-children @(rf/subscribe [::element.s/canvas-children])
        elements @(rf/subscribe [::document.s/elements])]
    (if (= state :default)
      [inner-sidebar-render canvas-children elements]
      (ra/with-let [canvas-children canvas-children
                    elements elements]
        [inner-sidebar-render canvas-children elements]))))

(defn root
  []
  [:> ContextMenu/Root
   [:> ContextMenu/Trigger {:class "flex h-full overflow-hidden"}
    [inner-sidebar]]
   [:> ContextMenu/Portal
    (into [:> ContextMenu/Content
           {:class "menu-content context-menu-content"
            :on-close-auto-focus #(.preventDefault %)}]
          (map (fn [item] [comp/context-menu-item item])
               comp/element-menu))]])
