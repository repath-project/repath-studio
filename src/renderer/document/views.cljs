(ns renderer.document.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.events :as-alias events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]
   [renderer.history.views :as history.views]
   [renderer.utils.system :as utils.system]
   [renderer.views :as views]))

(defn actions
  []
  (let [undos @(rf/subscribe [::history.subs/undos])
        redos @(rf/subscribe [::history.subs/redos])
        undos? @(rf/subscribe [::history.subs/undos?])
        redos? @(rf/subscribe [::history.subs/redos?])]
    [:div.toolbar

     [views/icon-button
      "file"
      {:title "New"
       :on-click #(rf/dispatch [::document.events/new])}]

     [views/icon-button
      "folder"
      {:title "Open"
       :on-click #(rf/dispatch [::document.events/open])}]

     [views/icon-button
      "save"
      {:title "Save"
       :on-click #(rf/dispatch [::document.events/save])
       :disabled @(rf/subscribe [::document.subs/active-saved?])}]

     [:span.v-divider]

     [:button.icon-button.items-center.px-1.gap-1.flex.w-auto
      {:title "Undo"
       :on-click #(rf/dispatch [::history.events/undo])
       :disabled (not undos?)}
      [views/icon "undo"]
      [history.views/select "Undo stack" undos (not undos?)]]

     [:button.icon-button.items-center.px-1.gap-1.flex.w-auto
      {:title "Redo"
       :on-click #(rf/dispatch [::history.events/redo])
       :disabled (not redos?)}
      [views/icon "redo"]
      [history.views/select "Redo stack" redos (not redos?)]]]))

(defn close-button
  [id saved]
  [:button.close.small
   {:key id
    :title "Close document"
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
        tabs @(rf/subscribe [::document.subs/tabs])]
    (cond-> [{:label "Close"
              :action [::document.events/close id true]}
             {:label "Close others"
              :action [::document.events/close-others id]
              :disabled? (empty? (rest tabs))}
             {:label "Close all"
              :action [::document.events/close-all]}
             {:label "Close saved"
              :action [::document.events/close-saved]}]
      utils.system/electron?
      (concat [{:type :separator}
               {:label "Open containing directory"
                :action [::document.events/open-directory path]
                :disabled? (nil? path)}]))))

(defn tab
  [id title active?]
  (reagent/with-let [dragged-over? (reagent/atom false)]
    (let [saved? @(rf/subscribe [::document.subs/saved? id])]
      [:> ContextMenu/Root
       [:> ContextMenu/Trigger
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
                     (let [dropped-id (-> (.-dataTransfer e) (.getData "id") uuid)]
                       (.preventDefault e)
                       (reset! dragged-over? false)
                       (rf/dispatch [::document.events/swap-position dropped-id id])))
          :ref (fn [this]
                 (when (and this active?)
                   (rf/dispatch [::events/scroll-into-view this])))}
         [:span.truncate.pointer-events-none title]
         [close-button id saved?]]]
       [:> ContextMenu/Portal
        (into
         [:> ContextMenu/Content
          {:class "menu-content context-menu-content"}]
         (map (fn [item]
                [views/context-menu-item item])
              (context-menu id)))]])))

(defn tab-bar
  []
  (let [documents @(rf/subscribe [::document.subs/entities])
        tabs @(rf/subscribe [::document.subs/tabs])
        active-id @(rf/subscribe [::document.subs/active-id])]
    [:div.flex.justify-between.gap-px
     [views/scroll-area
      [:div.flex.flex-1
       {:class "h-[41px]"}
       (for [document-id tabs]
         (let [title (:title (get documents document-id))
               active? (= document-id active-id)]
           ^{:key (str document-id)} [tab document-id title active?]))
       [:div.drag.flex-1]]]
     [:div.toolbar
      [:> DropdownMenu/Root
       [:> DropdownMenu/Trigger
        {:as-child true}
        [:button.button.flex.items-center.justify-center.aria-expanded:overlay.px-2.font-mono.rounded
         {:aria-label "More document actions"}
         [views/icon "ellipsis-h"]]]
       [:> DropdownMenu/Portal
        [:> DropdownMenu/Content
         {:class "menu-content rounded-sm"}
         (for [item [{:label "Close all"
                      :key :close-all
                      :action [::document.events/close-all]}
                     {:label "Close saved"
                      :key :close-saved
                      :action [::document.events/close-saved]}]]
           ^{:key (:key item)}
           [views/dropdown-menu-item item])
         [:> DropdownMenu/Arrow {:class "menu-arrow"}]]]]]]))
