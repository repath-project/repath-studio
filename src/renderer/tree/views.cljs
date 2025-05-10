(ns renderer.tree.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.events :as-alias app.events]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.frame.events :as-alias frame.events]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.tree.events :as-alias tree.events]
   [renderer.ui :as ui]
   [renderer.utils.element :as utils.element]
   [renderer.utils.keyboard :as utils.keyboard]))

(defn lock-button
  [id locked]
  [ui/icon-button
   (if locked "lock" "unlock")
   {:class ["list-item-action" (when-not locked "invisible")]
    :title (if locked "unlock" "lock")
    :on-double-click #(.stopPropagation %)
    :on-pointer-up #(.stopPropagation %)
    :on-click (fn [e]
                (.stopPropagation e)
                (rf/dispatch [::element.events/toggle-prop id :locked]))}])

(defn visibility-button
  [id visible]
  [ui/icon-button
   (if visible "eye" "eye-closed")
   {:class ["list-item-action" (when visible "invisible")]
    :title (if visible "hide" "show")
    :on-double-click #(.stopPropagation %)
    :on-pointer-up #(.stopPropagation %)
    :on-click (fn [e]
                (.stopPropagation e)
                (rf/dispatch [::element.events/toggle-prop id :visible]))}])

(defn set-item-label!
  [e id]
  (rf/dispatch-sync [::element.events/set-prop id :label (.. e -target -value)]))

(defn item-label
  [el]
  (let [{:keys [id label visible selected tag]} el
        properties (element.hierarchy/properties tag)
        tag-label (or (:label properties) (string/capitalize (name tag)))]
    (reagent/with-let [edit-mode? (reagent/atom false)]
      (if @edit-mode?
        [:input.mr-1.pl-0.bg-transparent.w-full
         {:class ["font-[inherit]! leading-[inherit]!"
                  (when (= :svg tag) "font-bold")]
          :default-value label
          :placeholder tag-label
          :auto-focus true
          :on-key-down #(utils.keyboard/input-key-down-handler! % label set-item-label! id)
          :on-blur (fn [e]
                     (reset! edit-mode? false)
                     (set-item-label! e id))}]
        [:div.flex.w-full
         [:div.truncate
          {:class [(when-not visible "opacity-60")
                   (when (= :svg tag) "font-bold")]
           :style {:cursor (when selected "text")}
           :on-double-click (fn [e]
                              (.stopPropagation e)
                              (reset! edit-mode? true))}
          (if (empty? label) tag-label label)]]))))

(defn drop-handler!
  [e parent-id]
  (let [id (-> (.-dataTransfer e) (.getData "id") uuid)]
    (.preventDefault e)
    (rf/dispatch [::element.events/set-parent id parent-id])))

(def last-focused-id (reagent/atom nil))

(defn set-last-focused-id!
  [id]
  (reset! last-focused-id id))

(defn key-down-handler!
  [e id]
  (case (.-key e)
    "ArrowUp"
    (do (.stopPropagation e)
        (rf/dispatch [::tree.events/focus-up id]))

    "ArrowDown"
    (do (.stopPropagation e)
        (rf/dispatch [::tree.events/focus-down id]))

    "ArrowLeft"
    (do (.stopPropagation e)
        (rf/dispatch [::document.events/collapse-el id]))

    "ArrowRight"
    (do (.stopPropagation e)
        (rf/dispatch [::document.events/expand-el id]))

    "Enter"
    (do (.stopPropagation e)
        (rf/dispatch [::element.events/select id (.-ctrlKey e)]))

    nil))

(defn collapse-button
  [id collapsed]
  [ui/icon-button
   (if collapsed "chevron-right" "chevron-down")
   {:title (if collapsed "expand" "collapse")
    :class "list-item-action"
    :on-pointer-up #(.stopPropagation %)
    :on-click #(rf/dispatch (if collapsed
                              [::document.events/expand-el id]
                              [::document.events/collapse-el id]))}])

(defn list-item-button
  [el {:keys [depth collapsed hovered]}]
  (let [{:keys [id selected children locked visible]} el
        collapse-button-width 22
        padding (* collapse-button-width (cond-> depth (seq children) dec))]
    [:div.button.list-item-button
     {:class [(when selected "selected")
              (when hovered "hovered")]
      :tab-index 0
      :data-id (str id)
      :on-double-click #(rf/dispatch [::frame.events/pan-to-element id])
      :on-pointer-enter #(rf/dispatch [::document.events/set-hovered-id id])
      :ref (fn [this]
             (when (and this selected)
               (rf/dispatch [::app.events/scroll-into-view this])
               (set-last-focused-id! (.getAttribute this "data-id"))))
      :draggable true
      :on-key-down #(key-down-handler! % id)
      :on-drag-start #(-> % .-dataTransfer (.setData "id" (str id)))
      :on-drag-enter #(rf/dispatch [::document.events/set-hovered-id id])
      :on-drag-over #(.preventDefault %)
      :on-drop #(drop-handler! % id)
      :on-pointer-down #(when (= (.-button %) 2)
                          (rf/dispatch [::element.events/select id (.-ctrlKey %)]))
      :on-pointer-up (fn [e]
                       (.stopPropagation e)
                       (if (.-shiftKey e)
                         (rf/dispatch-sync [::tree.events/select-range @last-focused-id id])
                         (do (rf/dispatch [::element.events/select id (.-ctrlKey e)])
                             (reset! last-focused-id id))))
      :style {:padding-left padding}}
     [:div.flex.items-center.content-between.w-full
      (when (seq children)
        [collapse-button id collapsed])
      [:div.flex-1.overflow-hidden.flex.items-center
       {:class "gap-1.5"}
       (when-let [icon (:icon (utils.element/properties el))]
         [ui/icon icon {:class (when-not visible "opacity-60")}])
       [item-label el]]
      [lock-button id locked]
      [visibility-button id visible]]]))

(defn item [el depth elements]
  (let [{:keys [selected children id]} el
        has-children (seq children)
        hovered-ids @(rf/subscribe [::document.subs/hovered-ids])
        collapsed-ids @(rf/subscribe [::document.subs/collapsed-ids])
        collapsed (contains? collapsed-ids id)]
    [:li {:class (when selected "overlay")
          :role "menuitem"}
     [list-item-button el {:depth depth
                           :collapsed collapsed
                           :hovered (contains? hovered-ids id)}]
     (when (and has-children (not collapsed))
       [:ul {:role "menu"}
        (for [el (mapv (fn [k] (get elements k)) (reverse children))]
          ^{:key (:id el)} [item el (inc depth) elements])])]))

(defn inner-sidebar-render
  [root-children elements]
  [:div.tree-sidebar
   {:on-pointer-up #(rf/dispatch [::element.events/deselect-all])}
   [ui/scroll-area
    [:ul {:role "menu"
          :on-pointer-leave #(rf/dispatch [::document.events/clear-hovered])
          :style {:width "227px"}}
     (for [el (reverse root-children)]
       ^{:key (:id el)} [item el 1 elements])]]])

(defn inner-sidebar []
  (let [state @(rf/subscribe [::tool.subs/state])
        root-children @(rf/subscribe [::element.subs/root-children])
        elements @(rf/subscribe [::document.subs/elements])]
    (if (= state :idle)
      [inner-sidebar-render root-children elements]
      (reagent/with-let [root-children root-children
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
               element.views/context-menu))]])
