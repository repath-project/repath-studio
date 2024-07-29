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
  [:div.p-5
   [:div.flex.gap-3.items-start.pb-2
    [:div
     [:div [:strong "Version: "] config/version]
     [:div [:strong "Browser: "] platform/user-agent]]]
   [:button.button.px-2.bg-primary.rounded.w-full
    {:auto-focus true
     :on-click #(rf/dispatch [::dialog.e/close])}
    "OK"]
   [close-button]])

(defn confirmation
  [{:keys [description action confirm-label cancel-label]}]
  [:div.p-5
   [:p description]
   [:div.flex.gap-2.flex-wrap
    [:button.button.px-2.bg-primary.rounded.flex-1
     {:on-click #(rf/dispatch [::dialog.e/close])}
     (or cancel-label "Cancel")]
    [:button.button.px-2.bg-primary.rounded.flex-1
     {:auto-focus true
      :on-click #(do (rf/dispatch [::dialog.e/close])
                     (rf/dispatch action))}
     (or confirm-label "OK")]]
   [close-button]])

(defn save
  [k]
  (let [document @(rf/subscribe [::document.s/document k])]
    [:div.p-5
     [:p
      "Your changes to " [:strong (:title document)]
      " will be lost if you close the document without saving."]
     [:div.flex.gap-2.flex-wrap
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
  (let [dialogs @(rf/subscribe [::dialog.s/dialogs])]
    [:> Dialog/Root
     {:open (seq dialogs)
      :on-open-change #(rf/dispatch [::dialog.e/close])}
     [:> Dialog/Portal
      [:> Dialog/Overlay {:class "backdrop"}]
      [:> Dialog/Content
       (merge {:class "dialog-content"
               :on-key-down #(.stopPropagation %)}
              (:attrs (last dialogs)))
       (when-let [title (:title (last dialogs))]
         [:> Dialog/Title
          {:class "text-xl px-5 pt-5"}
          title])
       [:> Dialog/Description
        {:class "m-0"}
        (:content (last dialogs))]]]]))
