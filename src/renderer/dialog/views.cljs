(ns renderer.dialog.views
  (:require
   ["@radix-ui/react-dialog" :as Dialog]
   ["cmdk" :as Command]
   [clojure.string :as str]
   [config :as config]
   [i18n :refer [t]]
   [platform :as platform]
   [re-frame.core :as rf]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.dialog.subs :as-alias dialog.s]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.menubar.views :as menubar]
   [renderer.ui :as ui]))

(defn about
  []
  [:div.p-5
   [:div.flex.gap-3.items-start.pb-2
    [:p
     [:span.block [:strong "Version: "] config/version]
     [:span.block [:strong "Browser: "] platform/user-agent]]]
   [:button.button.px-2.bg-primary.rounded.w-full
    {:auto-focus true
     :on-click #(rf/dispatch [::dialog.e/close])}
    "OK"]])

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
     (or confirm-label "OK")]]])

(defn save
  [{:keys [id title]}]
  [:div.p-5
   [:p
    "Your changes to " [:strong title]
    " will be lost if you close the document without saving."]
   [:div.flex.gap-2.flex-wrap
    [:button.button.px-2.bg-primary.rounded.flex-1
     {:on-click #(do (rf/dispatch [::dialog.e/close])
                     (rf/dispatch [::document.e/close id false]))}
     "Don't save"]
    [:button.button.px-2.bg-primary.rounded.flex-1
     {:on-click #(rf/dispatch [::dialog.e/close])}
     "Cancel"]
    [:button.button.px-2.bg-primary.rounded.flex-1
     {:auto-focus true
      :on-click #(do (rf/dispatch [::dialog.e/close])
                     (rf/dispatch [::document.e/save-and-close id]))}
     "Save"]]])

(defn cmdk-item
  [{:keys [label action icon type]}]
  (when-not (= type :separator)
    [:> Command/CommandItem
     {:on-select (fn []
                   (rf/dispatch [::dialog.e/close false])
                   (rf/dispatch action))}
     [:div.w-7.h-7.mr-2.rounded.line-height-6.flex.justify-center.items-center
      {:class (when icon "overlay")}
      (when icon [ui/icon icon {:class "icon"}])]
     label
     [:div.right-slot
      [ui/shortcuts action]]]))

(defn cmdk-group-inner
  [items label]
  (for [i items]
    (if (:items i)
      (cmdk-group-inner (:items i) (:label i))
      ^{:key (:id i)} [cmdk-item (update i :label #(str/join " - " (remove nil? [label %])))])))

(defn cmdk-group
  [{:keys [label items]}]
  [:> Command/CommandGroup
   {:heading label}
   (cmdk-group-inner items nil)])

(defn cmdk
  []
  [:> Command/Command
   {:label "Command Menu"
    :on-key-down #(.stopPropagation %)}
   [:> Command/CommandInput
    {:class "p-3 bg-secondary text-sm border-b border-default"
     :placeholder (t [:cmdk/search-command "Search for a command"])}]
   [ui/scroll-area
    [:> Command/CommandList
     {:class "p-1"}
     [:> Command/CommandEmpty
      (t [:cmdk/no-results "No results found."])]
     (for [i (menubar/root-menu)]
       ^{:key (:id i)}
       [cmdk-group i])]]])

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
          (cond->> title
            (string? title)
            (into [:div.text-xl.pl-5.pr-10.pt-5]))])
       (when (:close-button? (last dialogs))
         [:> Dialog/Close
          {:class "close-button small"
           :aria-label "Close"}
          [ui/icon "times"]])
       [:> Dialog/Description
        {:as-child true}
        [:div (:content (last dialogs))]]]]]))
