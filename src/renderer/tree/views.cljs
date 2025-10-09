(ns renderer.tree.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.event.impl.keyboard :as event.impl.keyboard]
   [renderer.events :as-alias events]
   [renderer.frame.events :as-alias frame.events]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.tree.effects :as tree.effects]
   [renderer.tree.events :as-alias tree.events]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.element :as utils.element]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn item-prop-toggle
  [id state k active-icon inactive-icon active-title inactive-title small?]
  (let [title (if state active-title inactive-title)]
    [views/icon-button
     (if state
       active-icon
       inactive-icon)
     {:class ["hover:bg-transparent text-inherit focus:outline-hidden
               focus:text-inherit active:text-inherit hover:text-inherit"
              (when (not state) (if small? "invisible" "opacity-30"))
              (when small? "small")]
      :title (t title)
      :on-double-click #(.stopPropagation %)
      :on-click (fn [e]
                  (.stopPropagation e)
                  (rf/dispatch [::element.events/toggle-prop id k title]))}]))

(defn set-item-label!
  [e id]
  (rf/dispatch-sync [::element.events/set-label id (.. e -target -value)])
  (when-not (.-relatedTarget e)
    (.focus (tree.effects/query-by-id! id))))

(defn item-label
  [el]
  (let [{:keys [id label visible selected tag]} el
        properties (element.hierarchy/properties tag)
        tag-label (or (:label properties)
                      (string/capitalize (name tag)))]
    (reagent/with-let [edit-mode? (reagent/atom false)]
      (if @edit-mode?
        [:input.bg-transparent.w-full
         {:class ["font-[inherit]! leading-[inherit]!"
                  (when (= :svg tag) "font-bold")]
          :default-value label
          :placeholder tag-label
          :auto-focus true
          :draggable true ; Prevents drag of parent.
          :enter-key-hint "done"
          :on-drag-start #(.preventDefault %)
          :on-focus #(.. % -target select)
          :on-key-down #(event.impl.keyboard/input-key-down-handler!
                         % label
                         set-item-label! id)
          :on-blur (fn [e]
                     (reset! edit-mode? false)
                     (set-item-label! e id))}]
        [:div.flex.w-full.overflow-hidden
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
  (let [id (utils.dom/event->uuid e)]
    (.preventDefault e)
    (rf/dispatch [::element.events/set-parent id parent-id])))

(def last-focused-id (reagent/atom nil))

(defn set-last-focused-id!
  [id]
  (reset! last-focused-id id))

(defn key-down-handler!
  [e id]
  (case (.-code e)
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

    "Space"
    (do (.stopPropagation e)
        (rf/dispatch [::element.events/select id (.-ctrlKey e)]))

    nil))

(defn collapse-button
  [id collapsed small?]
  [views/icon-button
   (if collapsed "chevron-right" "chevron-down")
   {:title (if collapsed "expand" "collapse")
    :class ["hover:bg-transparent text-inherit hover:text-inherit
             focus:outline-hidden rtl:scale-x-[-1]"
            (when small? "small")]
    :on-double-click #(.stopPropagation %)
    :on-click #(do (.stopPropagation %)
                   (rf/dispatch (if collapsed
                                  [::document.events/expand-el id]
                                  [::document.events/collapse-el id])))}])

(defn list-item-button
  [el {:keys [depth collapsed hovered small]}]
  (let [{:keys [id selected children locked visible]} el
        collapse-button-width (if small 21 33)
        padding (* collapse-button-width (cond-> depth (seq children) dec))]
    [:div.list-item-button.button.flex.px-1.items-center.text-start
     {:class ["hover:bg-overlay [&.hovered]:bg-overlay hover:[&_button]:visible"
              (when selected "accent")
              (when hovered "hovered")
              (when-not small "h-[45px]")]
      :tab-index 0
      :data-id (str id)
      :on-double-click #(rf/dispatch [::frame.events/pan-to-element id])
      :on-pointer-enter #(rf/dispatch [::document.events/set-hovered-id id])
      :ref (fn [this]
             (when (and this selected)
               (rf/dispatch [::events/scroll-into-view this])
               (set-last-focused-id! (.getAttribute this "data-id"))))
      :draggable true
      :on-key-down #(key-down-handler! % id)
      :on-drag-start #(-> % .-dataTransfer (.setData "id" (str id)))
      :on-drag-enter #(rf/dispatch [::document.events/set-hovered-id id])
      :on-drag-over #(.preventDefault %)
      :on-drop #(drop-handler! % id)
      :on-pointer-down #(when (= (.-button %) 2)
                          (rf/dispatch [::element.events/select id (.-ctrlKey %)]))
      :on-click (fn [e]
                  (.stopPropagation e)
                  (if (.-shiftKey e)
                    (rf/dispatch-sync [::tree.events/select-range @last-focused-id id])
                    (do (rf/dispatch [::element.events/select id (.-ctrlKey e)])
                        (reset! last-focused-id id))))}
     [:div.shrink-0 {:style {:flex-basis padding}}]
     [:div.flex-1.flex.items-center.justify-between.w-full.overflow-hidden
      (when (seq children)
        [collapse-button id collapsed small])
      [:div.flex-1.overflow-hidden.flex.items-center
       {:class "gap-1.5"}
       (when-let [icon (:icon (utils.element/properties el))]
         [views/icon icon {:class (when-not visible "opacity-60")}])
       [item-label el]]
      [item-prop-toggle id locked :locked
       "lock" "unlock"
       [::unlock "Unlock"] [::lock "Lock"]
       small]
      [item-prop-toggle id (not visible) :visible
       "eye-closed" "eye"
       [::show "Show"] [::hide "Hide"]
       small]]]))

(defn item [el depth elements small?]
  (let [{:keys [selected children id]} el
        has-children (seq children)
        hovered-ids @(rf/subscribe [::document.subs/hovered-ids])
        collapsed-ids @(rf/subscribe [::document.subs/collapsed-ids])
        collapsed (contains? collapsed-ids id)]
    [:li {:class (when selected "bg-overlay")
          :role "menuitem"}
     [list-item-button el {:depth depth
                           :collapsed collapsed
                           :hovered (contains? hovered-ids id)
                           :small small?}]
     (when (and has-children (not collapsed))
       [:ul {:role "menu"}
        (for [el (mapv (fn [k] (get elements k)) (reverse children))]
          ^{:key (:id el)}
          [item el (inc depth) elements small?])])]))

(defn inner-sidebar-render
  [root-children elements small?]
  [:div#tree-sidebar.flex.flex-1.bg-primary.h-full.overflow-hidden
   ;; When the tree is hovered, ignore the hovered class of the elements,
   ;; if the element itself is not also hovered.
   {:class "hover:**:[&.list-item-button]:not-hover:bg-inherit"
    :on-click #(rf/dispatch [::element.events/deselect-all])}
   [views/scroll-area
    [:ul {:role "menu"
          :on-pointer-leave #(rf/dispatch [::document.events/clear-hovered])
          :class (if small? "w-[227px]" "w-80")}
     (for [el (reverse root-children)]
       ^{:key (:id el)}
       [item el 1 elements small?])]]])

(defn inner-sidebar []
  (let [state @(rf/subscribe [::tool.subs/state])
        root-children @(rf/subscribe [::element.subs/root-children])
        elements @(rf/subscribe [::document.subs/elements])
        md? @(rf/subscribe [::window.subs/breakpoint? :md])]
    (if (= state :idle)
      [inner-sidebar-render root-children elements md?]
      (reagent/with-let [root-children root-children
                         elements elements]
        [inner-sidebar-render root-children elements md?]))))

(defn root
  []
  [:> ContextMenu/Root
   [:> ContextMenu/Trigger
    {:class "flex h-full overflow-hidden"}
    [inner-sidebar]]
   [:> ContextMenu/Portal
    (into [:> ContextMenu/Content
           {:class "menu-content context-menu-content"
            :on-escape-key-down #(.stopPropagation %)}]
          (map (fn [menu-item] [views/context-menu-item menu-item])
               (element.views/context-menu)))]])
