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
   [repath.studio.reepl.views :as repl]
   [repath.studio.filters :as filters]
   [repath.studio.history.views :as history]
   [re-frame.registrar]
   [reagent.core :as ra]
   ["react-color" :refer [PhotoshopPicker]]
   ["@fluentui/react" :as fui]
   [goog.string :as gstring]
   [goog.color :as color]))

(defn color-drip [color]
  [:div {:key      (keyword (str color))
         :on-click #((rf/dispatch [:set-fill color])
                     (rf/dispatch [:elements/set-attribute :fill (tools/rgba color) true]))

         :class "color-drip"
         :style    {:background-color (tools/rgba color)}}])

(defn color-swatch [colors]
  [:div.h-box {:style {:flex "1 1 100%"}} (map color-drip colors)])

(defn color-palette []
  (let [palette @(rf/subscribe [:color-palette])]
    (into [:div.v-box {:style {:margin-left styles/h-padding
                               :flex "1"
                               :height "33px"
                               :margin "1px"}}]
          (map color-swatch palette))))

(defn color-picker
  [fill stroke]
  (let [picker (ra/atom nil)]
    (fn [fill stroke]
      [:div {:style {:width    "48px"
                     :height   "48px"
                     :margin "2px"
                     :position "relative"}}
       [:button {:title    "Swap"
                 :class    "button"
                 :on-click #(rf/dispatch [:swap-colors])
                 :style    {:padding  0
                            :position "absolute"
                            :bottom   "-6px"
                            :left     "-6px"}} [comp/icon {:icon "swap"}]]
       [:div {:class "color-rect"
              :style {:background (tools/rgba stroke)
                      :bottom     0
                      :right      0}}
        [:div {:class "color-rect"
               :style {:width      "16px"
                       :height     "16px"
                       :bottom     "7px"
                       :right      "7px"
                       :background styles/level-2}}]]
       [:button {:class "color-rect"
                 :on-click #(if @picker
                              (reset! picker nil)
                              (reset! picker (.-target %)))
                 :style {:background (tools/rgba fill)}}]

       (when @picker [:> fui/Callout {:styles {:root {:padding "0" :z-index "1000"}}
                                      :onDismiss #(reset! picker nil)
                                      :target @picker}
                      [:> PhotoshopPicker
                       {:color (tools/rgba fill)
                        :on-change-complete #(rf/dispatch [:elements/set-attribute :fill (:hex (js->clj % :keywordize-keys true)) true])
                        :on-change #((rf/dispatch [:set-fill (vals (:rgb (js->clj % :keywordize-keys true)))])
                                     (rf/dispatch [:elements/set-attribute :fill (:hex (js->clj % :keywordize-keys true)) false]))}]])])))

(defn coordinates []
  (let [[x y] @(rf/subscribe [:adjusted-mouse-pos])]
    [:div.v-box {:style {:font-family  "Source Code Pro, monospace"
                         :margin-left styles/h-padding
                         :min-width "70px"}}
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
                         :elements/align-items "flex-end"}}
     [color-picker fill stroke]
     [color-palette]
     #_(when element-colors (map (fn [color] [color-drip (color/hexToRgb color)]) element-colors))
     [:select {:onChange #(rf/dispatch [:set-filter (-> % .-target .-value keyword)])
               :value    @(rf/subscribe [:filter])
               :style    {:background styles/level-3
                          :margin "2px"
                          :border-radius "4px"
                          :width "auto"}}
      [:option {:key :no-filter :value :no-filter} "No filter"]
      (map (fn [{:keys [id]}] [:option {:key id :value id} (name id)]) filters/accessibility)]
     [comp/radio-icon-button {:title "Grid" :active? @(rf/subscribe [:grid?]) :icon "grid" :action #(rf/dispatch [:toggle-grid])}]
     #_[comp/radio-icon-button {:title "Snap" :active? true :icon "magnet" :action #(rf/dispatch [:toggle-snap])}]
     [comp/radio-icon-button {:title "Rulers" :active? @(rf/subscribe [:rulers?]) :icon "ruler-combined" :action #(rf/dispatch [:toggle-rulers])}]
     [:div {::style {:position  "relative"}}

      [:button {:style {:font-family  "Source Code Pro, monospace"
                        :padding-left styles/h-padding
                        :padding-right 0
                        :height "32px"
                        :font-size "1em"
                        :background styles/level-3
                        :margin     "0 0 2px 4px"}
                :class "icon-button"} "Z: "]
      ^{:key zoom} [:input {:default-value (gstring/format "%.0f" (* 100 zoom))
                            :class    ["icon-button"]
                            :on-blur #(rf/dispatch [:set-zoom (/ (.. % -target -value) 100)])
                            :on-key-down #((when (= (.-keyCode %) 13) (rf/dispatch [:set-zoom (/ (.. % -target -value) 100)])))
                            :style {:font-family  "Source Code Pro, monospace"
                                    :min-width "40px"
                                    :padding-left 0
                                    :padding-right 0
                                    :height "32px"
                                    :font-size "1em"
                                    :background styles/level-3
                                    :margin "0"}}]
      [:button {:style {:font-family  "Source Code Pro, monospace"
                        :padding-left 0
                        :padding-right 0
                        :height "32px"
                        :font-size "1em"
                        :background styles/level-3
                        :margin "0"}
                :class "icon-button"} "%"]
      [:select {:onChange #(rf/dispatch [:set-zoom (-> % .-target .-value js/parseFloat)])
                :class    "icon-button"
                :value    zoom
                :style    {:background styles/level-3
                           :font-family  "Source Code Pro, monospace"
                           :margin-left "0"
                           :width "16px"
                           :font-size "1em"
                           :padding-left "0"
                           :padding-right "0"}}
       (let [zoom-options [0.1 0.5 1 2 5]]
         (when (not (contains? zoom-options zoom)) [:option {:key (str zoom) :value zoom} (str (* zoom 100) "%")])
         (map (fn [zoom-option] [:option {:key (str zoom-option) :value zoom-option} (str (* zoom-option 100) "%")]) zoom-options))]]
     #_[:<> [:button {:style {:font-family  "Source Code Pro, monospace"
                              :padding-left styles/h-padding
                              :padding-right 0
                              :height "32px"
                              :font-size "1em"
                              :background styles/level-3
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
                                      :background styles/level-3
                                      :margin     "2px 0 2px 0"}}]]
     [coordinates]]))

(defn tool-button [type selected?]
  (when (:icon (tools/properties type))
    [comp/radio-icon-button {:title type :active? selected? :icon (:icon (tools/properties type)) :action #(rf/dispatch [:set-tool type])}]))
  ;;  (when (descendants type)
  ;;    [:button {:key      (keyword (str "dropdown-" type))
  ;;              :title    type
  ;;              :class    ["icon-button" (when-not selected? "muted")]
  ;;              :style    {:background (when selected? styles/level-3)
  ;;                         :margin-left "0"
  ;;                         :width "16px"}
  ;;              :on-click #(rf/dispatch [:set-tool type])}
  ;;     [comp/icon {:icon "angle-down"}]])


(defn toolbar-group [group tool]
  (into [:div.h-box]
        (map #(tool-button % (= % tool))
             (descendants group))))

(def toolbars [::tools/transform
               ::tools/element
               ::tools/draw
               ::tools/edit])

(defn toolbar []
  (let [tool @(rf/subscribe [:tool])]
    (into [:div.h-box {:style {:justify-content "center" :padding "8px 16px 16px"}}]
          (interpose [:span.v-devider]
                     (map (fn [group] [toolbar-group group tool])
                          toolbars)))))

(defn command-input []
  [:div.v-box {:style {:background-color styles/level-0
                       :font-size "10px"
                       :position "relative"
                       :overflow "visible"}}
   [repl/main-view]])

(defn main-page []
  [:div {:style {:display "flex"
                 :flex 1
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
    [:h1 {:style {:margin "0"}} "RePath Studio"]
    [:h4 {:style {:margin "0"}} "Vector Graphics Manipulation"]
    [:h2 "Start"]
    [:div [:a {:on-click #(rf/dispatch [:documents/new])} "New"] [:span {:class "muted"} " (Ctrl+N)"]]
    [:div [:a {:on-click #(rf/dispatch [:documents/open])} "Open"] [:span {:class "muted"} " (Ctrl+O)"]]
    [:h2 "Recent"]
    [:h2 "Help"]
    [:div [:a {:on-click #(rf/dispatch [:window/open-remote-url "https://repath.studio/"])} "Website"]]
    [:div [:a {:on-click #(rf/dispatch [:window/open-remote-url "https://repath.studio/docs/getting-started/"])} "Getting Started"]]
    [:div [:a {:on-click #(rf/dispatch [:window/open-remote-url "https://github.com/sprocketc/repath-studio/"])} "Source Code"]]]])

(defn editor []
  [:div.h-box {:style {:flex "1"
                       :overflow "hidden"}}
   [:div.v-box {:style {:flex             "1"
                        :background-color styles/level-2}}
    [:<>
     [:div.h-box {:style {:flex 1}}
      [:div.v-box {:style {:flex 1}}
       [toolbar]
       (when @(rf/subscribe [:rulers?])  [:div.h-box
                                          [:div {:style {:width "22px"
                                                         :height "22px"
                                                         :text-align "center"
                                                         :border-right (str "1px solid " styles/border-color)
                                                         :border-bottom (str "1px solid " styles/border-color)}}]
                                                                                ;; [comp/toggle-icon-button {:active? @(rf/subscribe [:rulers-locked?])
                                                                                ;;                           :active-icon "lock"
                                                                                ;;                           :active-text "unlock"
                                                                                ;;                           :inactive-icon "unlock"
                                                                                ;;                           :inactive-text "lock"
                                                                                ;;                           :action #(rf/dispatch [:toggle-rulers-locked])}]

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
        [frame-canvas/frame]]
       [footer]]
      [history/tree]]
     [command-input]]]])

(defn main-panel []
  [:div.v-box {:style {:flex               "1"
                       :Webkit-user-select "none"
                       :height             "100vh"}}
   (when @(rf/subscribe [:header?]) [win/app-header])
   [:div.h-box {:style {:flex "1" :overflow "hidden"}}
    (when @(rf/subscribe [:tree?])     [:div.v-box {:class "sidebar"
                                                    :style {:flex (str "0 0 " @(rf/subscribe [:left-sidebar-width]))}}
                                        [docs/actions]
                                        (when (seq @(rf/subscribe [:documents])) [tree/tree-sidebar])])
    (if (seq @(rf/subscribe [:documents]))
      [:div.v-box {:style {:flex "1"}}
       [docs/tab-bar]

       [:div.h-box {:style {:flex "1"}}
        [editor]
        [:div.v-box {:class "sidebar"
                     :style {:flex (str "0 0 " @(rf/subscribe [:right-sidebar-width]))}}

         [:div.h-box {:style {:background-color styles/level-1
                              :box-sizing "border-box"
                              :flex "1"}}
          (when @(rf/subscribe [:properties?]) [attrs/form])
          [:div.v-box {:style {:flex "1"
                               :background-color styles/level-2
                               :padding "8px 4px"
                               :text-align "center"}}
           [comp/icon-button {:title "Bring To Front" :icon "bring-front" :action #(rf/dispatch [:elements/raise-to-top])}]
           [comp/icon-button {:title "Send To Back" :icon "send-back" :action #(rf/dispatch [:elements/lower-to-bottom])}]
           [comp/icon-button {:title "Bring Forward" :icon "bring-forward" :action #(rf/dispatch [:elements/raise])}]
           [comp/icon-button {:title "Send Backward" :icon "send-backward" :action #(rf/dispatch [:elements/lower])}]
           [:span.h-devider]
           [comp/icon-button {:title "Group" :icon "group" :action #(rf/dispatch [:group])}]
           [comp/icon-button {:title "Ungroup" :icon "ungroup" :action #(rf/dispatch [:ungroup])}]
           [:span.h-devider]
           [comp/icon-button {:title "Align Left" :icon "objects-align-left" :action #(rf/dispatch [:elements/align :left])}]
           [comp/icon-button {:title "Align Center Horizontaly" :icon "objects-align-center-horizontal" :action #(rf/dispatch [:elements/align :center-horizontal])}]
           [comp/icon-button {:title "Align Rignt" :icon "objects-align-right" :action #(rf/dispatch [:elements/align :right])}]
           [:span.h-devider]
           [comp/icon-button {:title "Align Top" :icon "objects-align-top" :action #(rf/dispatch [:elements/align :top])}]
           [comp/icon-button {:title "Align Center Verticaly" :icon "objects-align-center-vertical" :action #(rf/dispatch [:elements/align :center-vertical])}]
           [comp/icon-button {:title "Align Bottom" :icon "objects-align-bottom" :action #(rf/dispatch [:elements/align :bottom])}]
           [:span.h-devider]
           [comp/icon-button {:title "Distribute Spacing Horizontaly" :icon "distribute-spacing-horizontal" :action #(rf/dispatch [:elements/raise])}]
           [comp/icon-button {:title "Distribute Spacing Verticaly" :icon "distribute-spacing-vertical" :action #(rf/dispatch [:elements/lower])}]
           [:span.h-devider]
           [comp/icon-button {:title "Union" :icon "union" :action #(rf/dispatch [:elements/raise])}]
           [comp/icon-button {:title "Intersection" :icon "intersection" :action #(rf/dispatch [:elements/lower])}]
           [comp/icon-button {:title "Difference" :icon "difference" :action #(rf/dispatch [:elements/lower])}]
           [comp/icon-button {:title "Exclusion" :icon "exclusion" :action #(rf/dispatch [:elements/lower])}]
           [:span.h-devider]
           [comp/icon-button {:title "Distribute Spacing Horizontaly" :icon "rotate-clockwise" :action #(rf/dispatch [:elements/raise])}]
           [comp/icon-button {:title "Distribute Spacing Verticaly" :icon "rotate-counterclockwise" :action #(rf/dispatch [:elements/lower])}]
           [:span.h-devider]
           [comp/icon-button {:title "Distribute Spacing Horizontaly" :icon "flip-horizontal" :action #(rf/dispatch [:elements/raise])}]
           [comp/icon-button {:title "Distribute Spacing Verticaly" :icon "flip-vertical" :action #(rf/dispatch [:elements/lower])}]]]]]]
      [main-page])

    [menu/context-menu]]])
