(ns repath.studio.documents.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [repath.studio.components :as comp]
   [repath.studio.styles :as styles]
   [repath.studio.history.views :as history]
   [repath.studio.context-menu.views :refer [gen-menu]]))

(defn menu [e key]
  (let [document-tabs @(rf/subscribe [:document-tabs])]
    (gen-menu e [{:name "Close"
                  :shortcut "Ctrl+X"
                  :action [:document/close key]}
                 {:name "Close Others"
                  :shortcut "Ctrl+C"
                  :action (when (> (count document-tabs) 1) [:document/close-others key])}
                 {:name "Close Saved"
                  :shortcut "Ctrl+V"
                  :action [:document/close-saved]}
                 {:name "Close All"
                  :shortcut "Page Up"
                  :action [:document/close-all]}
                 :divider
                 {:name "Copy Path"
                  :shortcut "Page Down"
                  :action [:elements/lower]}
                 {:name "Copy Relative Path"
                  :shortcut "Home"
                  :action [:elements/raise-to-top]}
                 :divider
                 {:name "Open Containing Folder"
                  :action [:elements/lower]}])))

(defn actions []
  [:div.h-box {:style {:padding "1px" :overflow "visible"}}
   [comp/icon-button {:title "New" :icon "file" :action #(rf/dispatch [:document/new])}]
   [comp/icon-button {:title "Open" :icon "folder" :action #(rf/dispatch [:document/open])}]
   [comp/icon-button {:title "Save" :icon "save" :action #(rf/dispatch [:document/save])}]
   [:span.v-divider]
   [comp/icon-button {:title "Import" :icon "import" :class "disabled" :action #(rf/dispatch [:document/import])}]
   [comp/icon-button {:title "Export" :icon "export" :action #(rf/dispatch [:elements/export])}]
   [:span.v-divider]
   [comp/icon-button {:title "Undo" :icon "undo" :action #(rf/dispatch [:history/undo 1]) :disabled? (not @(rf/subscribe [:history/undos?]))}]
   [:select {:class "icon-button"
             :onChange #(rf/dispatch [:history/undo (-> % .-target .-value js/parseInt)])
             :disabled (not @(rf/subscribe [:history/undos?]))
             :style {:margin-left "-2px"
                     :max-width "14px"
                     :background styles/level-0
                     :font-size "1em"}}
    (history/select-options @(rf/subscribe [:history/undos]))]
   [comp/icon-button {:title "Undo" :icon "redo" :action #(rf/dispatch [:history/redo 1]) :disabled? (not @(rf/subscribe [:history/redos?]))}]
   [:select {:class "icon-button"
             :onChange #(rf/dispatch [:history/redo (-> % .-target .-value js/parseInt)])
             :disabled (not @(rf/subscribe [:history/redos?]))
             :style {:margin-left "-2px"
                     :max-width "14px"
                     :background styles/level-0
                     :font-size "1em"}}
    (history/select-options @(rf/subscribe [:history/redos]))]])

(defn close-button 
  [key active?]
  [:button {:key key
            :class "icon-button small close-document-button"
            :title "Close document"
            :style {:visibility (when active? "visible")}
            :on-mouse-down #(.stopPropagation %)
            :on-mouse-up #((.stopPropagation %)
                           (rf/dispatch [:document/close key]))} [comp/icon {:icon "times"}]])

(defn tab
  [key document active?]
  (let [dragged-over? (ra/atom false)]
    (fn [key document active?]
      [:div.h-box {:class "button document-tab"
                   :on-context-menu #(menu % key)
                   :on-wheel #(rf/dispatch [:document/scroll (.-deltaY %)])
                   :on-mouse-down #(case (.-buttons %)
                                     4 (rf/dispatch [:document/close key])
                                     1 (rf/dispatch [:set-active-document key]))
                   :draggable true
                   :on-drag-start #(.setData (.-dataTransfer %) "key" (apply str (rest (str key))))
                   :on-drag-over #(.preventDefault %)
                   :on-drag-enter #(reset! dragged-over? true)
                   :on-drag-leave #(reset! dragged-over? false)
                   :on-drop (fn [evt]
                              (.preventDefault evt)
                              (reset! dragged-over? false)
                              (rf/dispatch [:document/swap-position (keyword (.getData (.-dataTransfer evt) "key")) key]))
                   :style    {:background-color (if (or active? @dragged-over?) styles/level-2 styles/level-1)
                              :color (if active? styles/font-color styles/font-color-muted)}}
       [:span.document-name (:title document)]
       [close-button key active?]])))

(defn tab-bar []
  (let [documents @(rf/subscribe [:documents])
        document-tabs @(rf/subscribe [:document-tabs])
        active-document @(rf/subscribe [:active-document])]
    [:div.h-box {:style {:-webkit-app-region "drag"
                         :flex "0 0 40px"
                         :justify-content "space-between"}}
     [:div.h-box {:style {:flex "1 0 auto"}}
      (map (fn [document] ^{:key document} [tab document (document documents) (= document active-document)]) document-tabs)]
     [:div {:style {:padding "0 4px"}}
      [:button {:title    "Document Actions"
                :class    "icon-button"
                :style {:font-size "16px"
                        :padding styles/v-padding}} [comp/icon {:icon "ellipsis-h"}]]]]))
