(ns renderer.document.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.events :as-alias events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]
   [renderer.history.views :as history.views]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn actions
  []
  (let [undos @(rf/subscribe [::history.subs/undos])
        redos @(rf/subscribe [::history.subs/redos])
        md? @(rf/subscribe [::window.subs/breakpoint? :md])]
    [:div.toolbar

     [views/icon-button
      "file"
      {:title (t [::new "New"])
       :on-click #(rf/dispatch [::document.events/new])}]

     [views/icon-button
      "folder"
      {:title (t [::open "Open"])
       :on-click #(rf/dispatch [::document.events/open])}]

     [views/icon-button
      "save"
      {:title (t [::save "Save"])
       :on-click #(rf/dispatch [::document.events/save])
       :disabled @(rf/subscribe [::document.subs/active-saved?])}]

     [:span.v-divider]

     [history.views/action-button {:icon "undo"
                                   :title (t [::undo "Undo"])
                                   :options undos
                                   :show-options md?
                                   :action [::history.events/undo]}]

     [history.views/action-button {:icon "redo"
                                   :title (t [::redo "Redo"])
                                   :options redos
                                   :show-options md?
                                   :action [::history.events/redo]}]]))

(defn close-button
  [id saved]
  [:button.small.icon-button.invisible.relative.shrink-0.bg-inherit.group
   {:key id
    :title (t [::close-doc "Close document"])
    :on-click (fn [e]
                (.stopPropagation e)
                (rf/dispatch [::document.events/close id true]))}
   [views/icon "times"]
   (when-not saved
     [views/icon "dot"
      {:class "absolute inset-0 bg-inherit items-center text-foreground-muted
               sm:visible invisible group-hover:invisible group-focus:invisible
               group-active:invisible"}])])

(defn context-menu
  [id]
  (let [document @(rf/subscribe [::document.subs/entity id])
        path (:path document)
        tabs @(rf/subscribe [::document.subs/tabs])
        web? @(rf/subscribe [::app.subs/web?])]
    (cond-> [{:label (t [::close "Close"])
              :action [::document.events/close id true]}
             {:label (t [::close-others "Close others"])
              :action [::document.events/close-others id]
              :disabled (empty? (rest tabs))}
             {:label (t [::close-all "Close all"])
              :action [::document.events/close-all]}
             {:label (t [::close-saved "Close saved"])
              :action [::document.events/close-saved]}]
      (not web?)
      (concat [{:type :separator}
               {:label (t [::open-directory "Open containing directory"])
                :action [::document.events/open-directory path]
                :disabled (nil? path)}]))))

(defn tab
  [id]
  (let [document @(rf/subscribe [::document.subs/entity id])
        {:keys [title]} document
        active? (= id @(rf/subscribe [::document.subs/active-id]))]
    (reagent/with-let [dragged-over? (reagent/atom false)]
      (let [saved? @(rf/subscribe [::document.subs/saved? id])]
        [:> ContextMenu/Root
         [:> ContextMenu/Trigger
          {:as-child true}
          [:div.tab
           {:class ["flex items-center h-full relative text-left px-2 py-0
                     overflow-hidden hover:[&_button]:visible
                     hover:text-foreground"
                    (if active?
                      "bg-primary text-foreground [&_button]:visible"
                      "bg-secondary text-foreground-muted")
                    (when-not saved?
                      "[&_button]:visible")]
            :on-wheel #(when-not (zero? (.-deltaY %))
                         (rf/dispatch [::document.events/cycle (.-deltaY %)]))
            :on-click #(rf/dispatch [::document.events/set-active id])
            :on-pointer-up #(when (= (.-button %) 1)
                              (rf/dispatch [::document.events/close id true]))
            :draggable true
            :tab-index 0
            :on-key-down #(when (= (.-key %) "Enter")
                            (rf/dispatch [::document.events/set-active id]))
            :on-drag-start #(.setData (.-dataTransfer %) "id" (str id))
            :on-drag-over #(.preventDefault %)
            :on-drag-enter #(reset! dragged-over? true)
            :on-drag-leave #(reset! dragged-over? false)
            :on-drop (fn [e]
                       (let [dropped-id (utils.dom/event->uuid e)]
                         (.preventDefault e)
                         (reset! dragged-over? false)
                         (rf/dispatch [::document.events/swap-position
                                       dropped-id
                                       id])))
            :ref (fn [this]
                   (when (and this active?)
                     (rf/dispatch [::events/scroll-into-view this])))}
           [:div.pointer-events-none.px-2.gap-1.flex.overflow-hidden
            (when-not saved? [:span.sm:hidden "•"])
            [:span.truncate title]]
           [close-button id saved?]]]
         [:> ContextMenu/Portal
          (into
           [:> ContextMenu/Content
            {:class "menu-content context-menu-content"
             :on-escape-key-down #(.stopPropagation %)}]
           (map (fn [item]
                  [views/context-menu-item item])
                (context-menu id)))]]))))

(defn documents-dropdown-button
  []
  (let [documents @(rf/subscribe [::document.subs/entities])
        active-id @(rf/subscribe [::document.subs/active-id])
        md? @(rf/subscribe [::window.subs/breakpoint? :md])
        document-count (count documents)]
    [:> DropdownMenu/Root
     [:> DropdownMenu/Trigger
      {:as-child true}
      [:button.button.flex.items-center.justify-center.px-2.font-mono.rounded
       {:aria-label "More document actions"}
       [:div.flex.gap-1.items-center
        (when-not (or md? (= document-count 1))
          document-count)
        [views/icon (if md?
                      "ellipsis-h"
                      "chevron-down")]]]]
     [:> DropdownMenu/Portal
      (cond->> [{:label (t [::close-all "Close all"])
                 :key :close-all
                 :action [::document.events/close-all]}
                {:label (t [::close-saved "Close saved"])
                 :key :close-saved
                 :action [::document.events/close-saved]}]

        (and (seq documents)
             (not md?))
        (concat (mapv (fn [{:keys [id title]}]
                        {:key id
                         :type :checkbox
                         :label title
                         :action [::document.events/set-active id]
                         :checked (= active-id id)}) documents)
                [{:type :separator}])

        :always
        (into [:> DropdownMenu/Content
               {:side "bottom"
                :align "start"
                :class "menu-content rounded-sm"
                :on-key-down #(.stopPropagation %)
                :on-escape-key-down #(.stopPropagation %)}
               [:> DropdownMenu/Arrow {:class "fill-primary"}]]
              (map views/dropdown-menu-item)))]]))

(defn tab-bar
  []
  (let [documents @(rf/subscribe [::document.subs/entities])
        tabs @(rf/subscribe [::document.subs/tabs])
        active-id @(rf/subscribe [::document.subs/active-id])
        md? @(rf/subscribe [::window.subs/breakpoint? :md])
        tree-visible @(rf/subscribe [::app.subs/panel-visible? :tree])]
    [:div.flex.justify-between.gap-px
     [:div.flex.flex-1.overflow-hidden
      (if md?
        (for [document-id tabs]
          ^{:key document-id}
          [tab document-id])
        [:div.flex.overflow-hidden.gap-px
         (when (second documents)
           [:div.toolbar.bg-primary
            [documents-dropdown-button]])
         [tab active-id]])
      (when-not (and md? tree-visible)
        [actions])
      [:div.drag.flex-1]]

     (when md?
       [:div.toolbar
        [documents-dropdown-button]])]))
