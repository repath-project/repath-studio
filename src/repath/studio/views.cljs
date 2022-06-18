(ns repath.studio.views
  (:require
   [re-frame.core :as rf]
   [repath.studio.styles :as styles]
   [repath.studio.attrs.views :as attrs]
   [repath.studio.components :as comp]
   [repath.studio.tools.base :as tools]
   [repath.studio.window.views :as win]
   [repath.studio.tree.views :as tree]
   [repath.studio.documents.views :as docs]
   [repath.studio.canvas-frame.views :as frame-canvas]
   [repath.studio.rulers.views :as rulers]
   [repath.studio.context-menu.views :as menu]
   [repath.studio.codemirror.views :as cm]
   [repath.studio.reepl.views :as repl]
   [repath.studio.filters :as filters]
   [repath.studio.history.views :as history]
   [re-frame.registrar]
   [repath.studio.color.views :as color]
   ["@fluentui/react" :as fui]
   [goog.string :as gstring]))

(defn coordinates []
  (let [[x y] @(rf/subscribe [:adjusted-mouse-pos])]
    [:div.v-box {:style {:font-family  "Source Code Pro, monospace"
                         :margin-left styles/h-padding
                         :min-width "80px"}}
     [:div {:style {:display "flex"
                    :justify-content "space-between"}} [:span {:style {:margin-right "5px"}} "X:"] [:span (gstring/format "%.2f" x)]]
     [:div {:style {:display "flex"
                    :justify-content "space-between"}} [:span {:style {:margin-right "5px"}} "Y:"] [:span (gstring/format "%.2f" y)]]]))

(defn footer []
  (let [zoom @(rf/subscribe [:zoom])
        rotate @(rf/subscribe [:rotate])
        fill   @(rf/subscribe [:fill])
        stroke @(rf/subscribe [:stroke])
        element-colors @(rf/subscribe [:elements/colors])]
    [:div.h-box {:style {:padding styles/h-padding
                         :overflow "visible"
                         :background-color styles/level-2
                         :margin-top "1px"
                         :elements/align-items "flex-end"}}
     [color/picker fill stroke]
     [color/palette]
     #_(when element-colors (map (fn [color] [color-drip (color/hexToRgb color)]) element-colors))
     [:select {:onChange #(rf/dispatch [:document/set-filter (-> % .-target .-value keyword)])
               :value    @(rf/subscribe [:filter])
               :style    {:background styles/level-3
                          :margin "2px"
                          :border-radius "4px"
                          :width "auto"}}
      [:option {:key :no-filter :value :no-filter} "No filter"]
      (map (fn [{:keys [id]}] [:option {:key id :value id} (name id)]) filters/accessibility)]
     [comp/radio-icon-button {:title "Snap" :active? false :icon "magnet" :class "disabled" :action #(rf/dispatch [:document/toggle-snap])}]
     [comp/radio-icon-button {:title "Grid" :active? @(rf/subscribe [:grid?]) :icon "grid" :action #(rf/dispatch [:document/toggle-grid])}]
     [comp/radio-icon-button {:title "Rulers" :active? @(rf/subscribe [:rulers?]) :icon "ruler-combined" :action #(rf/dispatch [:document/toggle-rulers])}]
     #_[comp/radio-icon-button {:title "History tree" :class:active? @(rf/subscribe [:history?]) :icon "history" :action #(rf/dispatch [:document/toggle-history])}]
     [comp/radio-icon-button {:title "XML view" :active? @(rf/subscribe [:xml?]) :icon "code" :action #(rf/dispatch [:document/toggle-xml])}]
     [:div.h-box {:style {:align-items "center" :background styles/overlay :border-radius "4px" :margin "2px"}}
      [:span {:style {:font-family  "Source Code Pro, monospace"
                        :padding-left styles/h-padding
                        :font-size "1em"}} "Z: "]
      ^{:key zoom} [:input {:default-value (gstring/format "%.2f" (* 100 zoom))
                            :on-blur #(rf/dispatch [:set-zoom (/ (.. % -target -value) 100)])
                            :on-key-down #((when (= (.-keyCode %) 13) (rf/dispatch [:set-zoom (/ (.. % -target -value) 100)])))
                            :style {:font-family  "Source Code Pro, monospace"
                                    :width "60px"
                                    :background "transparent"
                                    :padding-left 0
                                    :line-height "1em"
                                    :padding-right 0
                                    :font-size "1em"
                                    :margin "0"}}]
      [:span {:style {:font-family  "Source Code Pro, monospace" :flex "0 0 10px"}} "%"]
      [:select {:onChange #(rf/dispatch [:set-zoom (-> % .-target .-value js/parseFloat)])
                :value    zoom
                :style    {:font-family  "Source Code Pro, monospace"
                           :margin-left "0"
                           :flex "0"
                           :width styles/icon-size
                           :background styles/level-3
                           :font-size "1em"
                           :padding-left "0"
                           :height (* styles/icon-size 2)
                           :padding-right "0"}}
       (let [zoom-options [0.1 0.5 1 2 5]]
         (when (not (contains? zoom-options zoom)) [:option {:key (str zoom) :value zoom} (str (* zoom 100) "%")])
         (map (fn [zoom-option] [:option {:key (str zoom-option) :value zoom-option} (str (* zoom-option 100) "%")]) zoom-options))]]
     #_[:<> [:button {:style {:font-family  "Source Code Pro, monospace"
                              :padding-left styles/h-padding
                              :padding-right 0
                              :height "32px"
                              :font-size "1em"
                              :background styles/overlay
                              :margin     "0 0 2px 4px"}
                      :class "icon-button"} "R: "]
        ^{:key zoom} [:input {:default-value rotate
                              :class    ["icon-button"]
                              :on-blur #(rf/dispatch [:set-rotate (.. % -target -value)])
                              :on-key-down #((when (= (.-keyCode %) 13) (rf/dispatch [:set-rotate (.. % -target -value)])))
                              :style {:font-family  "Source Code Pro, monospace"
                                      :min-width "40px"
                                      :padding-left 0
                                      :padding-right 0
                                      :height "32px"
                                      :font-size "1em"
                                      :background styles/overlay
                                      :margin     "2px 0 2px 0"}}]]
     [coordinates]]))

(defn tool-button [type selected?]
  (when (:icon (tools/properties type))
    [comp/radio-icon-button {:title type :active? selected? :icon (:icon (tools/properties type)) :action #(rf/dispatch [:set-tool type])}]))
  ;;  (when (descendants type)
  ;;    [:button {:key      (keyword (str "dropdown-" type))
  ;;              :title    type
  ;;              :class    ["icon-button" (when-not selected? "muted")]
  ;;              :style    {:background (when selected? styles/overlay)
  ;;                         :margin-left "0"
  ;;                         :width "16px"}
  ;;              :on-click #(rf/dispatch [:set-tool type])}
  ;;     [comp/icon {:icon "angle-down"}]])


(defn toolbar-group [group tool]
  (into [:div.h-box]
        (map #(tool-button % (= % tool))
             (descendants group))))

(def toolbars [::tools/transform
               ::tools/container
               ::tools/graphics
               ::tools/custom
               ::tools/draw
               ::tools/misc])

(defn toolbar []
  (let [tool @(rf/subscribe [:tool])]
    (into [:div.h-box {:style {:justify-content "center" :padding styles/padding :flex-wrap "wrap"}}]
          (interpose [:span.v-devider]
                     (map (fn [group] [toolbar-group group tool])
                          toolbars)))))

(defn command-input []
  [:div.v-box {:style {:background-color styles/level-0
                       :font-size "10px"
                       :user-select "text"
                       :position "relative"
                       :overflow "visible"}}
   [repl/main-view]])

(defn home-page []
  [:div.h-box {:style {:flex 1
                       :justify-content "center"
                       :min-height "100%"
                       :overflow "auto"}}
   [:div {:style {:font-size "150%"
                  :justify-content "center"
                  :width "100%"
                  :margin-right "300px"
                  :padding "100px"
                  :max-width "1200px"
                  :background  styles/level-1
                  :elements/align-self "center"}}
    [:h1 {:style {:margin "0"}} "repath.studio"]
    [:h4 {:style {:margin "0"}} "Scalable Vector Graphics Manipulation"]
    [:h2 "Start"]
    [:div [:a {:on-click #(rf/dispatch [:document/new])} "New"] [:span {:class "muted"} " (Ctrl+N)"]]
    [:div [:a {:on-click #(rf/dispatch [:document/open])} "Open"] [:span {:class "muted"} " (Ctrl+O)"]]
    [:h2 "Recent"]
    [:h2 "Help"]
    [:div [:a {:on-click #(rf/dispatch [:window/open-remote-url "https://repath.studio/"])} "Website"]]
    [:div [:a {:on-click #(rf/dispatch [:window/open-remote-url "https://repath.studio/docs/getting-started/"])} "Getting Started"]]
    [:div [:a {:on-click #(rf/dispatch [:window/open-remote-url "https://github.com/re-path/studio"])} "Source Code"]]]])

(defn editor []
  [:div.h-box {:style {:flex "1" :overflow "hidden"}}
   [:div.v-box {:style {:flex "1" :overflow "hidden"}}
    [:div.v-box {:style {:flex "1" :overflow "hidden"}}
     [:div.h-box {:style {:flex "1" :overflow "hidden"}}
      [:div.v-box {:style {:flex "1" :background-color styles/level-2}}
       [toolbar]
       (when @(rf/subscribe [:rulers?])  [:div.h-box
                                          [:div {:style {:width "22px"
                                                         :height "22px"
                                                         :border-right (str "1px solid " styles/border-color)
                                                         :border-bottom (str "1px solid " styles/border-color)}}
                                           [comp/toggle-icon-button {:active? @(rf/subscribe [:rulers-locked?])
                                                                     :active-icon "lock"
                                                                     :active-text "unlock"
                                                                     :inactive-icon "unlock"
                                                                     :inactive-text "lock"
                                                                     :class "small"
                                                                     :action #(rf/dispatch [:document/toggle-rulers-locked])}]]


                                          [rulers/ruler {:orientation :horizontal :size 23}]])
       [:div.h-box {:style {:flex 1
                            :position "relative"}}
        (when @(rf/subscribe [:rulers?]) [rulers/ruler {:orientation :vertical :size 23}])
        (when @(rf/subscribe [:command-palette?])
          [:div.command-palette
           [:> fui/ComboBox {:dropdownMaxWidth 300
                             :allowFreeform false
                             :autoComplete "on"
                             :styles {:input {:font-size "12px"}}
                             :options (mapv (fn [command] {:key (keyword command)
                                                           :text command
                                                           :styles {:optionText {:font-size "14px"}}}) (keys (:event @re-frame.registrar/kind->id->handler)))
                             :onChange (fn [_ key]
                                         (rf/dispatch-sync [(keyword (.-key key))]))}]])
        [frame-canvas/frame]]]
      (when @(rf/subscribe [:xml?]) (let [xml @(rf/subscribe [:elements/xml])] [:div.v-scroll {:style {:flex "0 1 30%"
                                                                                                        :height "100%"
                                                                                                       :padding styles/padding
                                                                                                       :background styles/level-2}} [cm/editor xml {:options {:mode "text/xml"
                                                                                                                                                              :readOnly true}}]]))
      (when @(rf/subscribe [:history?]) [:div.v-scroll {:style {:flex "0 1 30%"
                                                                :padding styles/padding
                                                                :background styles/level-2}}])]
     [footer]
     [history/tree]]
    [command-input]]])

(defn main-panel []
  [:div.v-box {:style {:flex "1" :height "100vh"}}   
   (when @(rf/subscribe [:window/header?]) [win/app-header])
   [:div.h-box {:on-drag-over (fn [evt]
                                (rf/dispatch [:window/on-drag evt]))
                :style {:flex "1" :overflow "hidden"}}
    (when @(rf/subscribe [:window/drag]) [:div.drag-overlay])
    (when @(rf/subscribe [:window/sidebar? :tree])
      [:div.v-box {:class "sidebar" :style {:flex (str "0 0 " @(rf/subscribe [:window/sidebar :tree]) "px")}}
       [docs/actions]
       (when (seq @(rf/subscribe [:documents])) [tree/tree-sidebar])])
    [comp/resizer :tree :left]
    (if (seq @(rf/subscribe [:documents]))
      [:div.v-box {:style {:flex "1" :overflow "hidden"}}
       [docs/tab-bar]
       [:div.h-box {:style {:flex "1" :overflow "hidden"}}
        [editor]
        [comp/resizer :properties :right]
        (when @(rf/subscribe [:window/sidebar? :properties])
          [:div.v-box {:class "sidebar"
                       :style {:flex (str "0 0 " @(rf/subscribe [:window/sidebar :properties]) "px")}}

           [:div.h-box {:style {:background-color styles/level-1
                                :box-sizing "border-box"
                                :flex "1"}}
            [attrs/form]]])
        [:div.v-box {:style {:flex "0"
                             :border-left (when-not @(rf/subscribe [:window/sidebar? :properties]) (str "1px solid " styles/level-1))
                             :background-color styles/level-2
                             :padding "8px 4px"
                             :text-align "center"}}
         [comp/icon-button {:title "Bring To Front" :icon "bring-front" :action #(rf/dispatch [:elements/raise-to-top])}]
         [comp/icon-button {:title "Send To Back" :icon "send-back" :action #(rf/dispatch [:elements/lower-to-bottom])}]
         [comp/icon-button {:title "Bring Forward" :icon "bring-forward" :action #(rf/dispatch [:elements/raise])}]
         [comp/icon-button {:title "Send Backward" :icon "send-backward" :action #(rf/dispatch [:elements/lower])}]
         [:span.h-devider]
         [comp/icon-button {:title "Group" :icon "group" :action #(rf/dispatch [:elements/group])}]
         [comp/icon-button {:title "Ungroup" :icon "ungroup" :action #(rf/dispatch [:elements/ungroup])}]
         [:span.h-devider]
         [comp/icon-button {:title "Align Left" :icon "objects-align-left" :action #(rf/dispatch [:elements/align :left])}]
         [comp/icon-button {:title "Align Center Horizontaly" :icon "objects-align-center-horizontal" :action #(rf/dispatch [:elements/align :center-horizontal])}]
         [comp/icon-button {:title "Align Rignt" :icon "objects-align-right" :action #(rf/dispatch [:elements/align :right])}]
         [:span.h-devider]
         [comp/icon-button {:title "Align Top" :icon "objects-align-top" :action #(rf/dispatch [:elements/align :top])}]
         [comp/icon-button {:title "Align Center Verticaly" :icon "objects-align-center-vertical" :action #(rf/dispatch [:elements/align :center-vertical])}]
         [comp/icon-button {:title "Align Bottom" :icon "objects-align-bottom" :action #(rf/dispatch [:elements/align :bottom])}]
         [:span.h-devider]
         [comp/icon-button {:title "Distribute Spacing Horizontaly" :icon "distribute-spacing-horizontal" :class "disabled" :action #(rf/dispatch [:elements/raise])}]
         [comp/icon-button {:title "Distribute Spacing Verticaly" :icon "distribute-spacing-vertical" :class "disabled" :action #(rf/dispatch [:elements/lower])}]
         [:span.h-devider]
         [comp/icon-button {:title "Unite" :icon "unite" :class "disabled" :action #(rf/dispatch [:elements/raise])}]
         [comp/icon-button {:title "Intersect" :icon "intersect" :class "disabled" :action #(rf/dispatch [:elements/lower])}]
         [comp/icon-button {:title "Subtract" :icon "subtract" :class "disabled" :action #(rf/dispatch [:elements/lower])}]
         [comp/icon-button {:title "Exclude" :icon "exclude" :class "disabled" :action #(rf/dispatch [:elements/lower])}]
         [:span.h-devider]
         [comp/icon-button {:title "Distribute Spacing Horizontaly" :icon "rotate-clockwise" :class "disabled" :action #(rf/dispatch [:elements/raise])}]
         [comp/icon-button {:title "Distribute Spacing Verticaly" :icon "rotate-counterclockwise" :class "disabled" :action #(rf/dispatch [:elements/lower])}]
         [:span.h-devider]
         [comp/icon-button {:title "Distribute Spacing Horizontaly" :icon "flip-horizontal" :class "disabled" :action #(rf/dispatch [:elements/raise])}]
         [comp/icon-button {:title "Distribute Spacing Verticaly" :icon "flip-vertical" :class "disabled" :action #(rf/dispatch [:elements/lower])}]]]]
      [home-page])

    [menu/context-menu]]])
