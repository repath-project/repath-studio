(ns renderer.document.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.document.events :as-alias e]
   [renderer.document.subs :as-alias s]
   [renderer.history.events :as-alias history.e]
   [renderer.history.subs :as-alias history.s]
   [renderer.history.views :as history.v]
   [renderer.ui :as ui]
   [renderer.utils.system :as system]))

(defn actions
  []
  (let [undos @(rf/subscribe [::history.s/undos])
        redos @(rf/subscribe [::history.s/redos])
        undos? @(rf/subscribe [::history.s/undos?])
        redos? @(rf/subscribe [::history.s/redos?])]
    [:div.toolbar

     [ui/icon-button
      "file"
      {:title "New"
       :on-click #(rf/dispatch [::e/new])}]

     [ui/icon-button
      "folder"
      {:title "Open"
       :on-click #(rf/dispatch [::e/open])}]

     [ui/icon-button
      "save"
      {:title "Save"
       :on-click #(rf/dispatch [::e/save])
       :disabled @(rf/subscribe [::s/active-saved?])}]

     [:span.v-divider]

     [:button.icon-button.items-center.px-1.gap-1.flex.w-auto
      {:title "Undo"
       :on-click #(rf/dispatch [::history.e/undo])
       :disabled (not undos?)}
      [ui/icon "undo"]
      [history.v/select "Undo stack" undos (not undos?)]]

     [:button.icon-button.items-center.px-1.gap-1.flex.w-auto
      {:title "Redo"
       :on-click #(rf/dispatch [::history.e/redo])
       :disabled (not redos?)}
      [ui/icon "redo"]
      [history.v/select "Redo stack" redos (not redos?)]]]))

(defn close-button
  [id saved]
  [:button.close.small
   {:key id
    :title "Close document"
    :on-pointer-down #(.stopPropagation %)
    :on-pointer-up (fn [e]
                     (.stopPropagation e)
                     (rf/dispatch [::e/close id true]))}
   [ui/icon "times"]
   (when-not saved
     [ui/icon "dot" {:class "dot"}])])

(defn context-menu
  [id]
  (let [document @(rf/subscribe [::s/entity id])
        path (:path document)
        tabs @(rf/subscribe [::s/tabs])]
    (cond-> [{:label "Close"
              :action [::e/close id true]}
             {:label "Close others"
              :action [::e/close-others id]
              :disabled? (empty? (rest tabs))}
             {:label "Close all"
              :action [::e/close-all]}
             {:label "Close saved"
              :action [::e/close-saved]}]
      system/electron?
      (concat [{:type :separator}
               {:label "Open containing directory"
                :action [::e/open-directory path]
                :disabled? (not (and path system/electron?))}]))))

(defn tab
  [id title active?]
  (ra/with-let [dragged-over? (ra/atom false)]
    (let [saved? @(rf/subscribe [::s/saved? id])]
      [:> ContextMenu/Root
       [:> ContextMenu/Trigger
        [:div.tab
         {:class [(when active? "active")
                  (when saved? "saved")]
          :on-wheel #(rf/dispatch [::e/cycle (.-deltaY %)])
          :on-pointer-down #(case (.-buttons %)
                              4 (rf/dispatch [::e/close id true])
                              1 (rf/dispatch [::e/set-active id])
                              nil)
          :draggable true
          :on-drag-start #(.setData (.-dataTransfer %) "id" (str id))
          :on-drag-over #(.preventDefault %)
          :on-drag-enter #(reset! dragged-over? true)
          :on-drag-leave #(reset! dragged-over? false)
          :on-drop (fn [evt]
                     (let [dropped-id (-> (.-dataTransfer evt) (.getData "id") uuid)]
                       (.preventDefault evt)
                       (reset! dragged-over? false)
                       (rf/dispatch [::e/swap-position dropped-id id])))}
         [:span.truncate.pointer-events-none title]
         [close-button id saved?]]]
       [:> ContextMenu/Portal
        (into
         [:> ContextMenu/Content
          {:class "menu-content context-menu-content"}]
         (map (fn [item]
                [ui/context-menu-item item])
              (context-menu id)))]])))

(defn tab-bar
  []
  (let [documents @(rf/subscribe [::s/entities])
        tabs @(rf/subscribe [::s/tabs])
        active-id @(rf/subscribe [::s/active-id])]
    [:div.flex.drag.justify-between
     [:div.flex.flex-1.gap-px.overflow-hidden
      (for [document-id tabs]
        (let [title (:title (get documents document-id))
              active? (= document-id active-id)]
          ^{:key (str document-id)} [tab document-id title active?]))]
     [:div.toolbar
      [:> DropdownMenu/Root
       [:> DropdownMenu/Trigger
        {:as-child true}
        [:button.button.flex.items-center.justify-center.aria-expanded:overlay.px-2.font-mono.rounded
         [ui/icon "ellipsis-h"]]]
       [:> DropdownMenu/Portal
        [:> DropdownMenu/Content
         {:class "menu-content rounded"}
         (for [item [{:label "Close all"
                      :key :close-all
                      :action [::e/close-all]}
                     {:label "Close saved"
                      :key :close-saved
                      :action [::e/close-saved]}]]
           ^{:key (:key item)}
           [ui/dropdown-menu-item item])
         [:> DropdownMenu/Arrow {:class "menu-arrow"}]]]]]]))
