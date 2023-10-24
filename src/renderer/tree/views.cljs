(ns renderer.tree.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   ["@radix-ui/react-context-menu" :as ContextMenu]))

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
     :action #(rf/dispatch [:elements/toggle-property key :visible?])}]
   [comp/toggle-icon-button
    {:active? locked?
     :active-icon "lock"
     :active-text "unlock"
     :inactive-icon "unlock"
     :inactive-text "lock"
     :class (when-not locked? "list-item-action")
     :action #(rf/dispatch [:elements/toggle-property key :locked?])}]])

(defn scroll-into-view
  [el]
  (.scrollIntoView el #js {:behavior "smooth"
                           :block "nearest"}))

(defn- set-item-name
  [event key]
  (rf/dispatch [:elements/set-property key :name (.. event -target -value)]))

(defn on-key-down-handler
  [event key value]
  (let [target (.-target event)]
    (case (.-keyCode event)
      13 (do (set-item-name event key)
             (.blur target))
      27 (do (.blur target)
             (set! (.-value target) value))
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
  [{:keys [key selected? collapsed? children tag] :as element} depth]
  (let [hovered-keys @(rf/subscribe [:document/hovered-keys])
        hovered? (contains? hovered-keys key)
        page? (= tag :page)
        active-page @(rf/subscribe [:elements/active-page])
        active-page? (and page? (= (:key element) (:key active-page)))
        multiple-selected? @(rf/subscribe [:elements/multiple-selected?])
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
                          (.setData "key" (apply str (rest key))))
      :on-drag-enter #(rf/dispatch [:document/set-hovered-keys #{key}])
      :on-drag-over #(.preventDefault %)
      :on-drop (fn [evt]
                 (.preventDefault evt)
                 (rf/dispatch [:elements/set-parent (-> (.-dataTransfer evt)
                                                        (.getData "key")
                                                        (keyword)) key]))
      :on-click (fn [evt]
                  (.stopPropagation evt)
                  (rf/dispatch [:elements/select (.-ctrlKey evt) element]))
      :style {:padding-left (when (not page?)
                              (- (* depth collapse-button-width)
                                 (if (seq children) collapse-button-width 0)))}}

     [:<>
      (when (and (seq children) (not page?))
        [comp/toggle-collapsed-button
         collapsed?
         #(rf/dispatch [:elements/toggle-property key :collapsed?])])
      [:<> [item-input element]]
      [item-buttons element]]]))

(defn item [{:keys [collapsed? selected? children] :as element} depth elements]
  (let [has-children? (seq children)]
    [:li {:class (when selected? "level-2")}
     [list-item-button element depth]
     (when (and has-children? (not collapsed?))
       [:ul (map
             (fn [element] [item element (inc depth) elements])
             (mapv (fn [key] (get elements key)) (reverse children)))])]))

(defn tree-sidebar []
  (let [page-elements @(rf/subscribe [:elements/pages])
        active-page @(rf/subscribe [:elements/active-page])
        active-page-children @(rf/subscribe [:elements/filter (:children active-page)])
        elements @(rf/subscribe [:document/elements])
        elements-collapsed? @(rf/subscribe [:window/elements-collapsed?])
        _symbols-collapsed? @(rf/subscribe [:window/symbols-collapsed?])
        pages-collapsed? @(rf/subscribe [:window/pages-collapsed?])
        _defs-collapsed? @(rf/subscribe [:window/defs-collapsed?])]
    [:div.flex.flex-col.flex-1.overflow-hidden.level-1.tree-sidebar
     {:on-click #(rf/dispatch [:elements/deselect-all])}
     #_[:div.button.tree-heading
        {:on-click #(rf/dispatch [:window/toggle-symbols-collapsed])}
        [comp/toggle-collapsed-button symbols-collapsed?]
        [:div.flex-1 "Symbols"]
        [comp/icon-button {:icon "square-minus"}]]

     #_[:div.v-scroll
      {:style {:flex (if symbols-collapsed? 0 "0 1 128px")}}]

     [:div.button.tree-heading
      {:on-click #(rf/dispatch [:window/toggle-pages-collapsed])}
      [comp/toggle-collapsed-button pages-collapsed?]
      [:div.flex-1 "Pages"]
      [comp/icon-button
       {:icon "page-plus"
        :action (fn [evt]
                  (.preventDefault evt)
                  (rf/dispatch-sync [:elements/add-page]))}]]

     [:div.v-scroll
      {:style {:flex (if pages-collapsed? 0 "0 1 128px")}}
      [:div
       {:on-mouse-leave #(rf/dispatch [:document/set-hovered-keys #{}])}
       (map (fn [element] [list-item-button element 0])
            (reverse page-elements))]]

     [:div.button.tree-heading
      {:on-click #(rf/dispatch [:window/toggle-elements-collapsed])}
      [comp/toggle-collapsed-button elements-collapsed?]
      [:div.flex-1 "Elements"]
      #_[comp/icon-button
         {:icon "folder-plus"
          :action #(do (.stopPropagation %)
                       (rf/dispatch [:elements/create {:type :g}]))}]
      [comp/icon-button {:icon "square-minus"}]]

     [:> ContextMenu/Root
      [:> ContextMenu/Trigger {:asChild true}
       [:div.v-scroll
        {:style {:flex (if elements-collapsed? 0 1)}}

        [:div
         {:on-mouse-leave #(rf/dispatch [:document/set-hovered-keys #{}])}
         [:ul (map (fn [element] [item element 1 elements])
                   (reverse active-page-children))]]]]
      [:> ContextMenu/Portal
       (into [:> ContextMenu/Content
              {:class "menu-content context-menu-content"}]
             (map (fn [item] [comp/context-menu-item item])
                  comp/element-menu))]]

     #_[:div.button.tree-heading
        {:on-click #(rf/dispatch [:window/toggle-defs-collapsed])}
        [comp/toggle-collapsed-button defs-collapsed?]
        [:div.flex-1 "Defs"]
        [comp/icon-button {:icon "square-minus"}]]

     #_[:div.v-scroll {:style {:flex (if defs-collapsed? 0 "0 1 128px")}}]]))
