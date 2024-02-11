(ns renderer.tree.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.components :as comp]
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
     :action #(rf/dispatch [:element/toggle-prop key :visible?])}]
   [comp/toggle-icon-button
    {:active? locked?
     :active-icon "lock"
     :active-text "unlock"
     :inactive-icon "unlock"
     :inactive-text "lock"
     :class (when-not locked? "list-item-action")
     :action #(rf/dispatch [:element/toggle-prop key :locked?])}]])

(defn- set-item-name
  [e k]
  (rf/dispatch [:element/set-prop k :name (.. e -target -value)]))

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
      [:span
       {:class (when-not visible? "text-disabled")
        :style {:cursor "text"}
        :on-double-click (fn [e]
                           (.stopPropagation e)
                           (reset! edit-mode? true))}
       (if (empty? name) tag name)])))

(defn drop-handler
  [e k]
  (let [parent-key (-> (.-dataTransfer e)
                       (.getData "key")
                       keyword)]
    (.preventDefault e)
    (rf/dispatch [:element/set-parent parent-key k])))

(defn padding
  [depth children?]
  (let [collapse-button-width 22]
    (- (* depth collapse-button-width)
       (if children? collapse-button-width 0))))

(defn list-item-button
  [{:keys [key selected? collapsed? children] :as el} depth]
  (let [hovered-keys @(rf/subscribe [:document/hovered-keys])
        hovered? (contains? hovered-keys key)
        multiple-selected? @(rf/subscribe [:element/multiple-selected?])]
    [:div.flex.button.list-item-button
     {:class [(when selected? "selected")
              (when hovered? "hovered")]
      :tab-index 0
      :role "menuitem"
      :on-double-click #(rf/dispatch [:pan-to-element key])
      :on-pointer-enter #(rf/dispatch [:document/set-hovered-keys #{key}])
      :ref (fn [this]
             (when (and this selected? hovered? (not multiple-selected?))
               (dom/scroll-into-view this)))
      :draggable true
      :on-drag-start #(-> (.-dataTransfer %)
                          (.setData "key" (name key)))
      :on-drag-enter #(rf/dispatch [:document/set-hovered-keys #{key}])
      :on-drag-over #(.preventDefault %)
      :on-drop #(drop-handler % key)
      :on-pointer-down (fn [e]
                         (when (= (.-button e) 2)
                           (rf/dispatch [:element/select key (.-ctrlKey e)])))
      :on-pointer-up (fn [e]
                       (.stopPropagation e)
                       (rf/dispatch [:element/select key (.-ctrlKey e)]))
      :style {:padding-left (padding depth (seq children))}}
     [:div.flex.items-center.content-between.w-full
      (when (seq children)
        [comp/toggle-collapsed-button
         collapsed?
         #(rf/dispatch [:element/toggle-prop key :collapsed?])])
      [:div.flex-1 [label el]]
      [item-buttons el]]]))

(defn item [{:keys [collapsed? selected? children] :as el} depth elements]
  (let [has-children? (seq children)]
    [:li {:class (when selected? "level-2")}
     [list-item-button el depth]
     (when (and has-children? (not collapsed?))
       [:ul (map (fn [el] [item el (inc depth) elements])
                 (mapv (fn [key] (get elements key)) (reverse children)))])]))

(defn key-down-handler
  [e]
  (let [ctrl? (.-ctrlKey e)]
    (case (.-key e)
      "ArrowUp" (rf/dispatch [:element/select-up ctrl?])
      "ArrowDown" (rf/dispatch [:element/select-down ctrl?])
      "ArrowLeft" (rf/dispatch [:element/collapse])
      "ArrowRight" (rf/dispatch [:element/expand]))))

(defn inner-sidebar []
  (let [page-elements @(rf/subscribe [:element/pages])
        elements @(rf/subscribe [:document/elements])]
    [:div.flex.flex-col.flex-1.overflow-hidden.level-1.tree-sidebar
     {:on-pointer-up #(rf/dispatch [:element/deselect-all])
      :on-key-down key-down-handler}

     [:div.v-scroll
      [:div
       {:on-pointer-leave #(rf/dispatch [:document/set-hovered-keys #{}])}
       [:ul (map (fn [el] [item el 1 elements])
                 (reverse page-elements))]]]]))

(defn root
  []
  [:> ContextMenu/Root
   [:> ContextMenu/Trigger {:class "flex h-full"}
    [inner-sidebar]]
   [:> ContextMenu/Portal
    (into [:> ContextMenu/Content
           {:class "menu-content context-menu-content"
            :on-close-auto-focus #(.preventDefault %)}]
          (map (fn [item] [comp/context-menu-item item])
               comp/element-menu))]])
