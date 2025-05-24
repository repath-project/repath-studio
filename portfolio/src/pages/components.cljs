(ns pages.components
  (:require
   [pages.icons :as icons]
   [portfolio.reagent-18 :refer-macros [defscene]]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [renderer.event.impl.keyboard :as event.impl.keyboard]
   [renderer.history.events :as-alias history.events]
   [renderer.ui :as ui]))

(defscene buttons
  :title "Buttons"
  :params (atom false)
  [store]
  [:div.toolbar.bg-primary
   [ui/icon-button
    "download"
    {:title "download"
     :on-click #(js/alert "Downloaded")}]
   [ui/icon-button
    "folder"
    {:title "open"
     :on-click #(js/alert "Opened")}]
   [ui/icon-button
    "save"
    {:title "save"
     :disabled true
     :on-click #(js/alert "Saved")}]
   [:span.v-divider]
   [ui/radio-icon-button
    "refresh"
    @store
    {:title "Replay"
     :on-click #(swap! store not)}]])

(defscene switch
  :title "Switch"
  :params (atom true)
  [store]
  [:div.toolbar.bg-primary.h-10.gap-2
   [ui/switch
    "Default"
    {:id "default-switch"
     :default-checked @store
     :on-checked-change (fn [v] (reset! store v))}]
   [ui/switch
    "Disabled"
    {:id "disabled-switch"
     :disabled true
     :default-checked @store
     :on-checked-change (fn [v] (reset! store v))}]
   [ui/switch
    "Custom"
    {:id "custom-switch"
     :class "data-[state=checked]:bg-cyan-500"
     :default-checked @store
     :on-checked-change (fn [v] (reset! store v))}]
   [:span.v-divider]
   [:div (str "State: " @store)]])

(defscene slider
  :title "Slider"
  :params (atom [25])
  [store]
  [:div.toolbar.bg-primary.flex.gap-2.px-2
   [:div.w-64.h-8
    [ui/slider
     {:min 0
      :max 50
      :step 1
      :default-value @store
      :on-value-change (fn [v] (reset! store v))}]]
   [:div.w-64.h-8
    [ui/slider
     {:min 0
      :max 50
      :step 1
      :disabled true
      :default-value @store
      :on-value-change (fn [v] (reset! store v))}]]
   [:span.v-divider]
   [:div (first @store)]])

(rf/dispatch [::rp/set-keydown-rules event.impl.keyboard/keydown-rules])

(defscene shortcut
  :title "Shortcut"
  [:div.toolbar.bg-primary.h-10.p-2.gap-2
   [:div.flex.gap-2 "Single" [ui/shortcuts [::history.events/undo]]]
   [:span.v-divider]
   [:div.flex.gap-2 "Multiple" [ui/shortcuts [::history.events/redo]]]
   [:span.v-divider]
   [:div.flex.gap-2 "No shortcuts" [ui/shortcuts [:event-id-with-no-shortcut]]]])

(defscene default
  :title "Icons"
  [:div.flex
   [:div.flex.flex-wrap.gap-2.p-3
    (for [icon-name icons/default] ^{:key icon-name} [:div {:title icon-name}
                                                      [ui/icon icon-name]])]
   [:div.flex.gap-2.p-3
    (for [icon-name icons/branded] ^{:key icon-name} [:div {:title icon-name}
                                                      [ui/icon icon-name]])]
   [:div.flex.p-3 [ui/icon "download" {:class "text-accent"}]]])
