(ns repath.studio.tree.views
  (:require
   [re-frame.core :as rf]
   [repath.studio.components :as comp]
   [repath.studio.styles :as styles]
   [repath.studio.elements.views :refer [element-menu]]))

(defn item-list [& children]
  (into [:ul {:style {:margin           0
                      :padding          0
                      :list-style       "none"
                      :background-color styles/level-1}}]
        children))

(defn item-buttons
  [{:keys [key locked? visible?]}]
  [:<>
   [comp/toggle-icon-button {:active? visible?
                             :active-icon "eye"
                             :active-text "hide"
                             :inactive-icon "eye-slash"
                             :inactive-text "show"
                             :class (when visible? "list-item-button")
                             :action #(rf/dispatch [:elements/toggle-property key :visible?])}]
   [comp/toggle-icon-button {:active? locked?
                             :active-icon "lock"
                             :active-text "unlock"
                             :inactive-icon "unlock"
                             :inactive-text "lock"
                             :class (when-not locked? "list-item-button")
                             :action #(rf/dispatch [:elements/toggle-property key :locked?])}]])

(defn page [{:keys [key type name visible? locked? selected?] :as element} active? hovered?]
  [:div.h-box {:key key
               :class ["button list-item page-item" (when selected? "selected") (when active? "active")] 
               :on-mouse-enter #(rf/dispatch [:document/set-hovered-keys #{key}])
               :on-click (fn [e]
                           (.stopPropagation e)
                           (rf/dispatch [:elements/select (or (.-ctrlKey e) (.-ctrlKey e)) element]))
               :on-double-click #(rf/dispatch [:pan-to-element key])
               :style    {:elements/align-items  "center"
                          :cursor       "pointer"
                          :padding      "4px 11px 4px 14px"
                          :background-color (when hovered? styles/level-2)}}
   [:input {:class "list-item-input"
            :style {:color (when-not visible? styles/font-color-muted)
                    :pointer-events (when (not selected?) "none")}
            :value name
            :placeholder type
            :on-double-click #(.stopPropagation %)
            :on-change #(rf/dispatch [:elements/set-property key :name (.. % -target -value) true])}]
   [item-buttons {:locked? locked? :visible? visible? :key key}]])

(defn item [{:keys [type name visible? collapsed? locked? selected? children key] :as element} depth hovered? elements]
  (let [has-children? (seq children)]
    [:li {:key key}
     [:div.h-box {:class ["button list-item" (when selected? "selected") (when visible? "text-muted")]
                  :on-double-click #(rf/dispatch [:pan-to-element key])
                  :on-mouse-enter #(rf/dispatch [:document/set-hovered-keys #{key}])
                  :draggable true
                  :on-drag-start #(.setData (.-dataTransfer %) "key" (apply str (rest (str key))))
                  :on-drag-enter #(rf/dispatch [:document/set-hovered-keys #{key}])
                  :on-drag-over #(.preventDefault %)
                  :on-drop (fn [evt]
                             (.preventDefault evt)
                             (rf/dispatch [:elements/set-parent (keyword (.getData (.-dataTransfer evt) "key")) key]))
                  :on-click (fn [evt]
                              (.stopPropagation evt)
                              (rf/dispatch [:elements/select (or (.-ctrlKey evt) (.-ctrlKey evt)) element]))
                  :style {:padding-left (- (* depth 20) (if has-children? 20 0))
                          :background-color (when hovered? styles/level-2)}}
      (when has-children?
        [comp/toggle-collapsed-button collapsed? #(rf/dispatch [:elements/toggle-property key :collapsed?])])
      ;;  [:span {:class "button icon-button"} [comp/icon {:icon        (:icon (tools/properties type))
      ;;                                                    :fixed-width true}]]
      [:input {:class "list-item-input"
               :style {:color (when-not visible? styles/font-color-muted)
                       :pointer-events (when (not selected?) "none")}
               :value name
               :placeholder type
               :on-double-click #(.stopPropagation %)
               :on-change #(rf/dispatch [:elements/set-property key :name (.. % -target -value) true])}]
      [item-buttons {:locked? locked? :visible? visible? :key key}]]
     (when (and has-children? (not collapsed?))
       [item-list (map  (fn [element] ^{:key (str (:key element) "bounds")} [item element (inc depth) hovered? selected? elements]) (mapv (fn [key] (get elements key)) children))])]))

(defn tree-sidebar []
  (let [page-elements @(rf/subscribe [:elements/pages])
        active-page @(rf/subscribe [:elements/active-page])
        active-page-children @(rf/subscribe [:elements/filter (:children active-page)])
        elements @(rf/subscribe [:elements])
        hovered-keys @(rf/subscribe [:hovered-keys])
        active-page @(rf/subscribe [:active-page])
        elements-collapsed? @(rf/subscribe [:window/elements-collapsed?])
        symbols-collapsed? @(rf/subscribe [:window/symbols-collapsed?])
        pages-collapsed? @(rf/subscribe [:window/pages-collapsed?])
        defs-collapsed? @(rf/subscribe [:window/defs-collapsed?])]
    [:div.v-box {:on-context-menu element-menu
                 :on-click #(rf/dispatch [:elements/deselect-all])
                 :style {:flex 1
                         :overflow "hidden"
                         :background-color styles/level-1}}
     [:div.button.tree-heading {:on-click #(rf/dispatch [:window/toggle-symbols-collapsed])}
      [comp/toggle-collapsed-icon symbols-collapsed?]
      [:div {:style {:flex 1}} "Symbols"]
      [comp/icon-button {:icon "square-minus"}]]
     [:div.v-scroll {:style {:flex (if symbols-collapsed? 0 "0 1 128px")
                             :transition "all .2s"}}]
     [:div.button.tree-heading {:on-click #(rf/dispatch [:window/toggle-pages-collapsed])}
      [comp/toggle-collapsed-icon pages-collapsed?]
      [:div {:style {:flex 1}} "Pages"]
      [comp/icon-button {:icon "page-plus" :action #(rf/dispatch [:set-tool :page])}]]
     [:div.v-scroll {:style {:flex (if pages-collapsed? 0 "0 1 128px")
                             :transition "all .2s"}}
      [:div {:on-mouse-leave #(rf/dispatch [:document/set-hovered-keys #{}])}
       (map (fn [element] ^{:key (:key page)} [page element (= (:key element) active-page) (contains? hovered-keys (:key element))]) page-elements)]]
     [:div.button.tree-heading {:on-click #(rf/dispatch [:window/toggle-elements-collapsed])}
      [comp/toggle-collapsed-icon elements-collapsed?]
      [:div {:style {:flex 1}} "Elements"]
      [comp/icon-button {:icon "folder-plus" :action  #((.stopPropagation %) (rf/dispatch [:elements/create {:type :g}]))}]
      [comp/icon-button {:icon "square-minus"}]]
     [:div.v-scroll {:style {:flex (if elements-collapsed? 0 1)
                             :transition "all .2s"}}
      [:div {:style {:visibility "visible"}
             :on-mouse-leave #(rf/dispatch [:document/set-hovered-keys #{}])}
       [item-list (map #(item % 1 (contains? hovered-keys (:key %)) elements) active-page-children)]]]
     [:div.button.tree-heading {:on-click #(rf/dispatch [:window/toggle-defs-collapsed])}
      [comp/toggle-collapsed-icon defs-collapsed?]
      [:div {:style {:flex 1}} "Defs"]
      [comp/icon-button {:icon "square-minus"}]]
     [:div.v-scroll {:style {:flex (if defs-collapsed? 0 "0 1 128px")
                             :transition "all .2s"}}]]))
