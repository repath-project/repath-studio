(ns renderer.dialog.views
  (:require
   ["@radix-ui/react-dialog" :as Dialog]
   [config]
   [platform]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.dialog.subs :as-alias dialog.s]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]))

(defn close-button
  []
  [:> Dialog/Close
   {:class "close-button small"
    :aria-label "Close"}
   [comp/icon "times"]])

(defn about
  []
  [:div.p-4
   [:div.flex.gap-3.items-start.pb-2
    [:div
     [:h1.text-3xl.font-light.mb-2 "Repath Studio"]
     [:div  [:strong "Version: "] config/version]
     [:div  [:strong "Browser: "] platform/user-agent]]]
   [:button.button.px-2.bg-primary.rounded.w-full
    {:auto-focus true
     :on-click #(rf/dispatch [::dialog.e/close])}
    "OK"]
   [close-button]])

#_(defn confirmation
    [{:keys [title description action]}]
    [:div.p-4
     [:h1.text-3xl.mb-2.font-light title]
     [:div description]
     [:div.flex.justify-end
      [:button.button.px-2.bg-primary.rounded
       {:on-click #(rf/dispatch [::dialog.e/close])}
       "No"]
      [:button.button.px-2.bg-primary.rounded
       {:auto-focus true
        :on-click #(do (rf/dispatch [::dialog.e/close])
                       (rf/dispatch action))}
       "Yes"]]
     [close-button]])

(defn save
  [k]
  (let [document @(rf/subscribe [::document.s/document k])]
    [:div.p-4
     [:h1.text-xl.mb-2
      "Do you want to save your changes?"]
     [:p
      "Your changes to " [:strong (:title document)]
      " will be lost if you close the document without saving."]
     [:div.flex.gap-2
      [:button.button.px-2.bg-primary.rounded.flex-1
       {:on-click #(do (rf/dispatch [::dialog.e/close])
                       (rf/dispatch [::document.e/close k false]))}
       "Don't save"]
      [:button.button.px-2.bg-primary.rounded.flex-1
       {:on-click #(rf/dispatch [::dialog.e/close])}
       "Cancel"]
      [:button.button.px-2.bg-primary.rounded.flex-1
       {:auto-focus true
        :on-click #(do (rf/dispatch [::dialog.e/close])
                       (rf/dispatch [::document.e/save-and-close]))}
       "Save"]]
     [close-button]]))

(defn root
  []
  (let [dialog @(rf/subscribe [::dialog.s/dialog])]
    [:> Dialog/Root
     {:open dialog
      :on-open-change #(rf/dispatch [::dialog.e/close])}
     [:> Dialog/Portal
      [:> Dialog/Overlay {:class "dialog-overlay"}]
      [:> Dialog/Content
       (merge {:class "dialog-content"}
              (:attrs dialog))
       (:content dialog)]]]))
