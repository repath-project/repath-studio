(ns renderer.tree.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.app.events :as-alias app.e]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.events :as-alias element.e]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.s]
   [renderer.element.views :as element.v]
   [renderer.frame.events :as-alias frame.e]
   [renderer.tool.subs :as-alias tool.s]
   [renderer.tree.events :as-alias e]
   [renderer.ui :as ui]
   [renderer.utils.element :as element]
   [renderer.utils.keyboard :as keyb]))

(defn lock-button
  [id locked]
  [ui/icon-button
   (if locked "lock" "unlock")
   {:class (when-not locked "list-item-action")
    :title (if locked "unlock" "lock")
    :on-double-click #(.stopPropagation %)
    :on-pointer-up #(.stopPropagation %)
    :on-click (fn [e]
                (.stopPropagation e)
                (rf/dispatch [::element.e/toggle-prop id :locked]))}])

(defn visibility-button
  [id visible]
  [ui/icon-button
   (if visible "eye" "eye-closed")
   {:class (when visible "list-item-action")
    :title (if visible "hide" "show")
    :on-double-click #(.stopPropagation %)
    :on-pointer-up #(.stopPropagation %)
    :on-click (fn [e]
                (.stopPropagation e)
                (rf/dispatch [::element.e/toggle-prop id :visible]))}])

(defn set-item-label!
  [e id]
  (rf/dispatch-sync [::element.e/set-prop id :label (.. e -target -value)]))

(defn item-label
  [el]
  (let [{:keys [id label visible tag]} el
        properties (element.hierarchy/properties tag)
        tag-label (or (:label properties) (str/capitalize (name tag)))]
    (ra/with-let [edit-mode? (ra/atom false)]
      (if @edit-mode?
        [:input.list-item-input
         {:default-value label
          :placeholder tag-label
          :auto-focus true
          :on-key-down #(keyb/input-key-down-handler! % label set-item-label! id)
          :on-blur (fn [e]
                     (reset! edit-mode? false)
                     (set-item-label! e id))}]
        [:div.flex.w-full
         [:div.truncate
          {:class [(when-not visible "opacity-60")
                   (when (= :svg tag) "font-bold")]
           :style {:cursor "text"}
           :on-double-click (fn [e]
                              (.stopPropagation e)
                              (reset! edit-mode? true))}
          (if (empty? label) tag-label label)]]))))

(defn drop-handler!
  [e parent-id]
  (let [id (-> (.-dataTransfer e)
               (.getData "id")
               uuid)]
    (.preventDefault e)
    (rf/dispatch [::element.e/set-parent id parent-id])))

(defn padding
  [depth has-children]
  (let [collapse-button-width 22]
    (- (* depth collapse-button-width)
       (if has-children collapse-button-width 0))))

(def last-focused-id (ra/atom nil))

(defn set-last-focused-id!
  [id]
  (reset! last-focused-id id))

(defn key-down-handler!
  [e id]
  (case (.-key e)
    "ArrowUp"
    (do (.stopPropagation e)
        (rf/dispatch [::e/focus-up id]))

    "ArrowDown"
    (do (.stopPropagation e)
        (rf/dispatch [::e/focus-down id]))

    "ArrowLeft"
    (do (.stopPropagation e)
        (rf/dispatch [::document.e/collapse-el id]))

    "ArrowRight"
    (do (.stopPropagation e)
        (rf/dispatch [::document.e/expand-el id]))

    "Enter"
    (do (.stopPropagation e)
        (rf/dispatch [::element.e/select id (.-ctrlKey e)]))

    nil))

(defn toggle-collapsed-button
  [id collapsed]
  [ui/icon-button
   (if collapsed "chevron-right" "chevron-down")
   {:title (if collapsed "expand" "collapse")
    :on-pointer-up #(.stopPropagation %)
    :on-click #(rf/dispatch (if collapsed
                              [::document.e/expand-el id]
                              [::document.e/collapse-el id]))}])

(defn list-item-button
  [el {:keys [depth collapsed hovered]}]
  (let [{:keys [id selected children locked visible]} el]
    [:div.button.list-item-button
     {:class [(when selected "selected")
              (when hovered "hovered")]
      :tab-index 0
      :data-id (str id)
      :role "menuitem"
      :on-double-click #(rf/dispatch [::frame.e/pan-to-element id])
      :on-pointer-enter #(rf/dispatch [::document.e/set-hovered-id id])
      :ref (fn [this]
             (when (and this selected)
               (rf/dispatch [::app.e/scroll-into-view this])
               (set-last-focused-id! (.getAttribute this "data-id"))))
      :draggable true
      :on-key-down #(key-down-handler! % id)
      :on-drag-start #(-> % .-dataTransfer (.setData "id" (str id)))
      :on-drag-enter #(rf/dispatch [::document.e/set-hovered-id id])
      :on-drag-over #(.preventDefault %)
      :on-drop #(drop-handler! % id)
      :on-pointer-down #(when (= (.-button %) 2)
                          (rf/dispatch [::element.e/select id (.-ctrlKey %)]))
      :on-pointer-up (fn [e]
                       (.stopPropagation e)
                       (if (.-shiftKey e)
                         (rf/dispatch-sync [::e/select-range @last-focused-id id])
                         (do (rf/dispatch [::element.e/select id (.-ctrlKey e)])
                             (reset! last-focused-id id))))
      :style {:padding-left (padding depth (seq children))}}
     [:div.flex.items-center.content-between.w-full
      (when (seq children)
        [toggle-collapsed-button id collapsed])
      [:div.flex-1.overflow-hidden.flex.items-center
       {:class "gap-1.5"}
       (when-let [icon (:icon (element/properties el))]
         [ui/icon icon {:class (when-not visible "opacity-60")}])
       [item-label el]]
      [lock-button id locked]
      [visibility-button id visible]]]))

(defn item [el depth elements]
  (let [{:keys [selected children id]} el
        has-children (seq children)
        hovered-ids @(rf/subscribe [::document.s/hovered-ids])
        collapsed-ids @(rf/subscribe [::document.s/collapsed-ids])
        collapsed (contains? collapsed-ids id)]
    [:li {:class (when selected "overlay")}
     [list-item-button el {:depth depth
                           :collapsed collapsed
                           :hovered (contains? hovered-ids id)}]
     (when (and has-children (not collapsed))
       [:ul (for [el (mapv (fn [k] (get elements k)) (reverse children))]
              ^{:key (:id el)} [item el (inc depth) elements])])]))

(defn inner-sidebar-render
  [root-children elements]
  [:div.tree-sidebar
   {:on-pointer-up #(rf/dispatch [::element.e/deselect-all])}
   [ui/scroll-area
    [:ul {:on-pointer-leave #(rf/dispatch [::document.e/clear-hovered])
          :style {:width "227px"}}
     (for [el (reverse root-children)]
       ^{:key (:id el)} [item el 1 elements])]]])

(defn inner-sidebar []
  (let [state @(rf/subscribe [::tool.s/state])
        root-children @(rf/subscribe [::element.s/root-children])
        elements @(rf/subscribe [::document.s/elements])]
    (if (= state :idle)
      [inner-sidebar-render root-children elements]
      (ra/with-let [root-children root-children
                    elements elements]
        [inner-sidebar-render root-children elements]))))

(defn root
  []
  [:> ContextMenu/Root
   [:> ContextMenu/Trigger
    {:class "flex h-full overflow-hidden"}
    [inner-sidebar]]
   [:> ContextMenu/Portal
    (into [:> ContextMenu/Content
           {:class "menu-content context-menu-content"
            :on-close-auto-focus #(.preventDefault %)}]
          (map (fn [menu-item] [ui/context-menu-item menu-item])
               element.v/context-menu))]])
