(ns renderer.history.views
  (:require
   ["@radix-ui/react-select" :as Select]
   ["react" :as react]
   ["react-d3-tree" :refer [Tree]]
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]))

(defn select-option
  [{:keys [id explanation]}]
  [:> Select/Item
   {:value (str id)
    :class "menu-item px-2"}
   [:> Select/ItemText (apply t explanation)]])

(defn select
  [label options open]
  [:> Select/Root
   {:on-value-change #(rf/dispatch [::history.events/go-to (uuid %)])
    :on-open-change #(reset! open %)
    :disabled (empty? options)}
   [:> Select/Trigger
    {:aria-label label
     :as-child true}
    [:div.w-4.m-0.bg-transparent.flex.items-center.hover:pt-1
     {:class "min-h-[inherit]"}
     [:> Select/Value ""]
     [:> Select/Icon
      [views/icon "chevron-down"]]]]
   [:> Select/Portal
    [:> Select/Content
     {:side "bottom"
      :alignOffset -24
      :position "popper"
      :class "menu-content rounded-sm select-content"
      :on-key-down #(.stopPropagation %)
      :on-escape-key-down #(.stopPropagation %)}
     [:> Select/ScrollUpButton
      {:class "select-scroll-button"}
      [views/icon "chevron-up"]]
     [:> Select/Viewport
      {:class "select-viewport"}
      (into [:> Select/Group]
            (map select-option options))]
     [:> Select/ScrollDownButton
      {:class "select-scroll-button"}
      [views/icon "chevron-down"]]
     [:> Select/Arrow {:class "fill-primary"}]]]])

(defn action-button
  [args]
  (let [{:keys [icon title options action show-options]} args]
    (reagent/with-let [open (reagent/atom false)]
      [:button.button.rounded-sm.items-center.px-1.gap-1.flex.w-auto
       {:title title
        :class [(if show-options "px-1" "px-2")
                (when @open "bg-overlay!")]
        :on-click #(rf/dispatch action)
        :disabled (empty? options)}
       [views/icon icon]
       (when show-options
         [select "Undo stack" options open])])))

(defn node
  "https://bkrem.github.io/react-d3-tree/docs/interfaces/CustomNodeElementProps.html"
  [^js/CustomNodeElementProps props]
  (let [datum (.-nodeDatum props)
        active? (.-active datum)
        id (uuid (.-id datum))
        color (if active? "var(--color-accent)" (.-color datum))]
    (reagent/as-element
     [:circle.transition-fill
      {:class "hover:stroke-accent"
       :on-click #(rf/dispatch [::history.events/go-to id])
       :on-pointer-enter #(when-not active?
                            (rf/dispatch [::history.events/preview id]))
       :on-pointer-leave #(rf/dispatch [::history.events/reset-state id])
       :cx "0"
       :cy "0"
       :stroke color
       :stroke-width 4
       :fill color
       :r 18}
      [:title (.-name datum)]])))

(defn on-update
  "https://bkrem.github.io/react-d3-tree/docs/interfaces/TreeProps.html#onupdate"
  [target]
  (let [translate (.-translate target)
        zoom (.-zoom target)]
    (when (and translate zoom)
      (let [x (.-x translate)
            y (.-y translate)]
        (rf/dispatch-sync [::history.events/tree-view-updated zoom [x y]])))))

(defn center
  [ref]
  (when-let [dom-el (.-current ref)]
    (matrix/div [(.-clientWidth dom-el)
                 (.-clientHeight dom-el)] 2)))

(defn tree
  [ref]
  (let [tree-data @(rf/subscribe [::history.subs/tree-data])
        zoom @(rf/subscribe [::history.subs/zoom])
        dom-el (.-current ref)
        [x y] @(rf/subscribe [::history.subs/translate])
        translate #js {:x (or x (when dom-el (/ (.-clientWidth dom-el) 2)))
                       :y (or y (when dom-el (/ (.-clientHeight dom-el) 2)))}]
    [:> Tree
     {:data tree-data
      :collapsible false
      :orientation "vertical"
      :rootNodeClassName "root-node"
      :depthFactor -100
      :zoom zoom
      :translate translate
      :onUpdate on-update
      :separation #js {:nonSiblings 1
                       :siblings 1}
      :renderCustomNodeElement node}]))

(defn clear-dialog
  []
  {:title (t [::action-cannot-undone "This action cannot be undone."])
   :description (t [::clear-history-description
                    "Are you sure you wish to clear the document history?"])
   :confirm-label (t [::clear-history "Clear history"])
   :confirm-action [::history.events/clear]})

(defn root
  []
  (let [ref (react/createRef)]
    [:div.flex.flex-col.h-full
     [:div.flex-1 {:ref ref}
      [tree ref]]
     [:div.flex.p-1
      [:button.button.flex-1
       {:on-click #(rf/dispatch [::history.events/tree-view-updated
                                 0.5 (center ref)])}
       (t [::center-view "Center view"])]
      [:button.button.flex-1
       {:on-click #(rf/dispatch [::dialog.events/show-confirmation
                                 (clear-dialog)])}
       (t [::clear-history "Clear history"])]]]))
