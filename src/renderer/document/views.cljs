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
        undos? @(rf/subscribe [::history.subs/undos?])
        redos? @(rf/subscribe [::history.subs/redos?])
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

     [:button.icon-button.items-center.px-1.gap-1.flex.w-auto
      {:title (t [::undo "Undo"])
       :class (if md? "px-1" "px-2")
       :on-click #(rf/dispatch [::history.events/undo])
       :disabled (not undos?)}
      [views/icon "undo"]
      (when md?
        [history.views/select "Undo stack" undos (not undos?)])]

     [:button.icon-button.items-center.px-1.gap-1.flex.w-auto
      {:title (t [::redo "Redo"])
       :class (if md? "px-1" "px-2")
       :on-click #(rf/dispatch [::history.events/redo])
       :disabled (not redos?)}
      [views/icon "redo"]
      (when md?
        [history.views/select "Redo stack" redos (not redos?)])]]))

(defn close-button
  [id saved]
  [:button.close.small
   {:key id
    :title (t [::close-doc "Close document"])
    :on-click (fn [e]
                (.stopPropagation e)
                (rf/dispatch [::document.events/close id true]))}
   [views/icon "times"]
   (when-not saved
     [views/icon "dot" {:class "dot"}])])

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
  [id title active?]
  (reagent/with-let [dragged-over? (reagent/atom false)]
    (let [saved? @(rf/subscribe [::document.subs/saved? id])]
      [:> ContextMenu/Root
       [:> ContextMenu/Trigger
        {:as-child true}
        [:div.tab
         {:class [(when active? "active")
                  (when saved? "saved")]
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
         [:span.truncate.pointer-events-none.px-2 title]
         [close-button id saved?]]]
       [:> ContextMenu/Portal
        (into
         [:> ContextMenu/Content
          {:class "menu-content context-menu-content"
           :on-escape-key-down #(.stopPropagation %)}]
         (map (fn [item]
                [views/context-menu-item item])
              (context-menu id)))]])))

(defn more-button
  []
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger
    {:as-child true}
    [:button.button.flex.items-center.justify-center.px-2.font-mono.rounded
     {:class "aria-expanded:overlay"
      :aria-label "More document actions"}
     [views/icon "ellipsis-h"]]]
   [:> DropdownMenu/Portal
    [:> DropdownMenu/Content
     {:class "menu-content rounded-sm"
      :on-key-down #(.stopPropagation %)
      :on-escape-key-down #(.stopPropagation %)}
     (for [item [{:label (t [::close-all "Close all"])
                  :key :close-all
                  :action [::document.events/close-all]}
                 {:label (t [::close-saved "Close saved"])
                  :key :close-saved
                  :action [::document.events/close-saved]}]]
       ^{:key (:key item)}
       [views/dropdown-menu-item item])
     [:> DropdownMenu/Arrow {:class "fill-primary"}]]]])

(defn documents-dropdown-button
  [documents active-id]
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger
    {:as-child true}
    [:button.button.flex.items-center.px-1.justify-center.font-mono.rounded
     {:class "aria-expanded:overlay"
      :aria-label "Open documents"}
     [:div.flex.gap-1.items-center
      (count documents)
      [views/icon "chevron-down"]]]]
   [:> DropdownMenu/Portal
    [:> DropdownMenu/Content
     {:class "menu-content rounded-sm"
      :on-key-down #(.stopPropagation %)
      :on-escape-key-down #(.stopPropagation %)}
     (for [{:keys [id title]} documents]
       ^{:key id}
       [views/dropdown-menu-item {:key id
                                  :type :checkbox
                                  :label title
                                  :action [::document.events/set-active id]
                                  :checked (= active-id id)}])
     [:> DropdownMenu/Arrow {:class "fill-primary"}]]]])

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
        (for [document-id tabs
              :let [title (get-in documents [document-id :title])
                    active? (= document-id active-id)]]
          ^{:key document-id}
          [tab document-id title active?])
        [:div.flex.overflow-hidden.gap-px
         [tab active-id (get-in documents [active-id :title]) true]
         (when (second documents)
           [:div.toolbar.bg-primary
            [documents-dropdown-button (vals documents) active-id]])])
      (when-not (and md? tree-visible)
        [actions])
      [:div.drag.flex-1]]

     [:div.toolbar [more-button]]]))
