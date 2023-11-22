(ns renderer.tree.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   [re-frame.core :as rf]
   [renderer.components :as comp]))

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
     :action #(rf/dispatch [:element/toggle-property key :visible?])}]
   [comp/toggle-icon-button
    {:active? locked?
     :active-icon "lock"
     :active-text "unlock"
     :inactive-icon "unlock"
     :inactive-text "lock"
     :class (when-not locked? "list-item-action")
     :action #(rf/dispatch [:element/toggle-property key :locked?])}]])

(defn scroll-into-view
  [el]
  (.scrollIntoView el #js {:behavior "smooth"
                           :block "nearest"}))

(defn- set-item-name
  [e k]
  (rf/dispatch [:element/set-property k :name (.. e -target -value)]))

(defn on-key-down-handler
  [e k v]
  (let [target (.-target e)]
    (case (.-keyCode e)
      13 (do (set-item-name e k)
             (.blur target))
      27 (do (.blur target)
             (set! (.-value target) v))
      nil)))

(defn item-input
  [{:keys [tag name visible? selected? key]}]
  [:input.list-item-input
   {:class [(when-not visible? "text-disabled")
            (when-not selected? "pointer-events-none")]
    :default-value name
    :placeholder tag
    :on-key-down #(on-key-down-handler % key name)
    :on-double-click #(.stopPropagation %)
    :on-blur #(set-item-name % key)}])

(defn list-item-button
  [{:keys [key selected? collapsed? children tag] :as el} depth]
  (let [hovered-keys @(rf/subscribe [:document/hovered-keys])
        hovered? (contains? hovered-keys key)
        page? (= tag :page)
        active-page @(rf/subscribe [:element/active-page])
        active-page? (and page? (= (:key el) (:key active-page)))
        multiple-selected? @(rf/subscribe [:element/multiple-selected?])
        collapse-button-width 22]
    [:div.flex.button.list-item-button
     {:class [(when selected? "selected")
              (when hovered? "hovered")
              (when page? "page-item")
              (when active-page? "active")]
      :on-double-click #(rf/dispatch [:pan-to-element key])
      :on-mouse-enter #(rf/dispatch [:document/set-hovered-keys #{key}])
      :ref (fn [this]
             (when (and this selected? hovered? (not multiple-selected?))
               (scroll-into-view this)))
      :draggable true
      :on-drag-start #(-> (.-dataTransfer %)
                          (.setData "key" (name key)))
      :on-drag-enter #(rf/dispatch [:document/set-hovered-keys #{key}])
      :on-drag-over #(.preventDefault %)
      :on-drop (fn [e]
                 (.preventDefault e)
                 (rf/dispatch [:element/set-parent (-> (.-dataTransfer e)
                                                       (.getData "key")
                                                       keyword) key]))
      :on-click (fn [e]
                  (.stopPropagation e)
                  (rf/dispatch [:element/select (.-ctrlKey e) el]))
      :style {:padding-left (when-not page?
                              (- (* depth collapse-button-width)
                                 (if (seq children) collapse-button-width 0)))}}

     [:<>
      (when (and (seq children) (not page?))
        [comp/toggle-collapsed-button
         collapsed?
         #(rf/dispatch [:element/toggle-property key :collapsed?])])
      [:<> [item-input el]]
      [item-buttons el]]]))

(defn item [{:keys [collapsed? selected? children] :as el} depth elements]
  (let [has-children? (seq children)]
    [:li {:class (when selected? "level-2")}
     [list-item-button el depth]
     (when (and has-children? (not collapsed?))
       [:ul (map
             (fn [el] [item el (inc depth) elements])
             (mapv (fn [key] (get elements key)) (reverse children)))])]))

(defn tree-sidebar []
  (let [page-elements @(rf/subscribe [:element/pages])
        active-page @(rf/subscribe [:element/active-page])
        active-page-children @(rf/subscribe [:element/filter (:children active-page)])
        elements @(rf/subscribe [:document/elements])
        elements-collapsed? @(rf/subscribe [:tree/elements-collapsed?])
        pages-collapsed? @(rf/subscribe [:tree/pages-collapsed?])]
    [:div.flex.flex-col.flex-1.overflow-hidden.level-1.tree-sidebar
     {:on-click #(rf/dispatch [:element/deselect-all])}

     [:div.button.tree-heading
      {:on-click #(rf/dispatch [:tree/toggle-pages-collapsed])}
      [comp/toggle-collapsed-button pages-collapsed?]
      [:div.flex-1 "Pages"]
      [comp/icon-button "page-plus"
       {:on-click (fn [evt]
                    (.stopPropagation evt)
                    (rf/dispatch-sync [:element/add-page]))}]]

     [:div.v-scroll
      {:style {:flex (if pages-collapsed? 0 "0 1 128px")}}
      [:div
       {:on-mouse-leave #(rf/dispatch [:document/set-hovered-keys #{}])}
       (map (fn [el] [list-item-button el 0])
            (reverse page-elements))]]

     [:div.button.tree-heading
      {:on-click #(rf/dispatch [:tree/toggle-elements-collapsed])}
      [comp/toggle-collapsed-button elements-collapsed?]
      [:div.flex-1 "Elements"]
      [comp/icon-button "square-minus"]]

     [:> ContextMenu/Root
      [:> ContextMenu/Trigger {:asChild true}
       [:div.v-scroll
        {:style {:flex (if elements-collapsed? 0 1)}}

        [:div
         {:on-mouse-leave #(rf/dispatch [:document/set-hovered-keys #{}])}
         [:ul (map (fn [el] [item el 1 elements])
                   (reverse active-page-children))]]]]
      [:> ContextMenu/Portal
       (into [:> ContextMenu/Content
              {:class "menu-content context-menu-content"}]
             (map (fn [item] [comp/context-menu-item item])
                  comp/element-menu))]]]))
