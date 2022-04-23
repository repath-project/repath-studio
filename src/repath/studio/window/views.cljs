(ns repath.studio.window.views
  (:require
   [re-frame.core :as rf]
   [repath.studio.components :as comp]
   [repath.studio.styles :as styles]
   ["@fluentui/react" :as fui]))

(def menu [{:key :file
            :label "File"
            :items [{:key :new-file
                     :text "New File"
                     :secondaryText "Ctrl+N"
                     :onClick #(rf/dispatch [:document/new])}
                    {:key :new-window
                     :text "New Window"
                     :secondaryText "Ctrl+Shift+N"}
                    {:key :devider-1
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :save
                     :text "Save"
                     :secondaryText "Ctrl+S"
                     :onClick #(rf/dispatch [:document/save])}
                    {:key :save-as
                     :text "Save As"
                     :secondaryText "Ctrl+Shift+S"}
                    {:key :save-all
                     :text "Save All"
                     :onClick #(rf/dispatch [:document/save-all])}
                    {:key :devider-2
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :auto-save
                     :canCheck true
                     :isChecked true
                     :text "Autosave"}
                    {:key :devider-3
                     :itemType fui/ContextualMenuItemType.Divider}
                    {:key :exit
                     :text "Exit"}]}
           {:key :edit
            :label "Edit"
            :items [{:key :new
                     :text "New File"
                     :secondaryText "Ctrl+N"}]}
           {:key :view
            :label "View"
            :items [{:key :new
                     :text "New File"
                     :secondaryText "Ctrl+N"}]}
           {:key :object
            :label "Object"
            :items [{:key :new
                     :text "New File"
                     :secondaryText "Ctrl+N"}]}
           {:key :help
            :label "Help"
            :items [{:key :new
                     :text "New File"
                     :secondaryText "Ctrl+N"}]}])

(defn menu-button [{:keys [key label items]}]
  [:> fui/DefaultButton {:key key
                         :text label
                         :class "muted"
                         :styles {:root {:border 0 
                                         :min-width "auto"
                                         :padding "0 10px"}
                                  :menuIcon {:display "none"}}
                         :menuProps {:shouldFocusOnMount true
                                     :shouldFocusOnContainer true
                                     :items items}}])
  ;; [:button  {:key   key
  ;;            :class "button muted"
  ;;            :style {:padding styles/padding}} label]
  

(defn window-controls
  "We could have used a different icon for :window/toggle-maximized based on :window/maximized? state,
   but electron's api seems to behave inconsistently after version 13 (tested in linux)."
  []
  [:div {:style {:flex "1 1 100%"
                 :-webkit-app-region "drag"
                 :text-align "right"}}
   [:button {:class    "button muted window-control-button"
             :on-click #(rf/dispatch [:window/minimize])} [comp/icon {:icon "window-minimize"}]]
   [:button {:class "button muted window-control-button"
             :on-click #(rf/dispatch [:window/toggle-maximized])} [comp/icon {:icon "window-restore"}]]
   [:button {:class    "button muted window-control-button"
             :on-click #(rf/dispatch [:window/close])} [comp/icon {:icon "times"}]]])

(defn title-bar []
  (let [title @(rf/subscribe [:title])]
    [:div.h-box {:class "title-bar"}  title]))

(defn app-header []
  (when-not @(rf/subscribe [:window/fullscreen?]) [:div.h-box
                                                   [:div.h-box {:style {:flex "1 1 100%"
                                                                        :-webkit-app-region "drag"}}
                                                    [:img {:src "img/icon-no-bg.svg"
                                                           :style {:padding styles/padding
                                                                   :width styles/icon-size
                                                                   :height styles/icon-size}}] (map menu-button menu)] [title-bar] [window-controls]]))
