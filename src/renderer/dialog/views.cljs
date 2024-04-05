(ns renderer.dialog.views
  (:require
   ["@radix-ui/react-dialog" :as Dialog]
   [config]
   [platform]
   [re-frame.core :as rf]
   [renderer.components :as comp]))

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
    {:on-click #(rf/dispatch [:dialog/close])}
    "OK"]
   [close-button]])

(defn confirmation
  [{:keys [title description action]}]
  [:div.p-4
   [:h1.text-3xl.mb-2.font-light title]
   [:div description]
   [:div.flex.justify-end
    [:button.button.px-2.bg-primary.rounded
     {:on-click #(rf/dispatch [:dialog/close])}
     "No"]
    [:button.button.px-2.bg-primary.rounded
     {:on-click #(do (rf/dispatch [:dialog/close])
                     (rf/dispatch action))}
     "Yes"]]
   [close-button]])

(defn save
  [k]
  (let [document @(rf/subscribe [:document/document k])]
    [:div.p-4
     [:h1.text-xl.mb-2
      "Do you want to save your changes?"]
     [:p
      "Your changes to " [:strong (:title document)]
      " will be lost if you close the document without saving."]
     [:div.flex.gap-2
      [:button.button.px-2.bg-primary.rounded.flex-1
       {:on-click #(do (rf/dispatch [:dialog/close])
                       (rf/dispatch [:document/close k false]))}
       "Don't save"]
      [:button.button.px-2.bg-primary.rounded.flex-1
       {:on-click #(rf/dispatch [:dialog/close])}
       "Cancel"]
      [:button.button.px-2.bg-primary.rounded.flex-1
       {:on-click #(do (rf/dispatch [:dialog/close])
                       (rf/dispatch [:document/save-and-close]))}
       "Save"]]
     [close-button]]))

(defn root
  []
  (let [dialog @(rf/subscribe [:dialog])]
    [:> Dialog/Root
     {:open dialog
      :on-open-change #(rf/dispatch [:dialog/close])}
     [:> Dialog/Portal
      [:> Dialog/Overlay {:class "dialog-overlay"}]
      [:> Dialog/Content
       (merge {:class "dialog-content"}
              (:attrs dialog))
       (:content dialog)]]]))
