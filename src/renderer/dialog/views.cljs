(ns renderer.dialog.views
  (:require
   ["@radix-ui/react-dialog" :as Dialog]
   ["cmdk" :as Command]
   [clojure.string :as string]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.subs :as app.subs]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.dialog.subs :as-alias dialog.subs]
   [renderer.document.events :as-alias document.events]
   [renderer.menubar.views :as menubar.views]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]))

(defn about
  []
  (let [user-agent @(rf/subscribe [::app.subs/user-agent])]
    [:div.p-5
     [:div.flex.gap-3.items-start.pb-2
      [:p
       [:span.block [:strong "Version: "] config/version]
       [:span.block [:strong "Browser: "] user-agent]]]
     [:button.button.px-2.accent.rounded.w-full
      {:auto-focus true
       :on-click #(rf/dispatch [::dialog.events/close])}
      "OK"]]))

(defn confirmation
  [{:keys [description action confirm-label cancel-label]}]
  [:div.p-5
   [:p description]
   [:div.flex.gap-2.flex-wrap
    [:button.button.px-2.bg-primary.rounded.flex-1
     {:on-click #(rf/dispatch [::dialog.events/close])}
     (or cancel-label "Cancel")]
    [:button.button.px-2.rounded.flex-1.accent
     {:auto-focus true
      :on-click #(rf/dispatch [::dialog.events/close action])}
     (or confirm-label "OK")]]])

(defn save
  [{:keys [id title]}]
  [:div.p-5
   [:p
    "Your changes to " [:strong title]
    " will be lost if you close the document without saving."]
   [:div.flex.gap-2.flex-wrap
    [:button.button.px-2.bg-primary.rounded.flex-1
     {:on-click #(rf/dispatch [::dialog.events/close
                               [::document.events/close id false]])}
     "Don't save"]
    [:button.button.px-2.bg-primary.rounded.flex-1
     {:on-click #(rf/dispatch [::dialog.events/close])}
     "Cancel"]
    [:button.button.px-2.rounded.flex-1.accent
     {:auto-focus true
      :on-click #(rf/dispatch [::dialog.events/close
                               [::document.events/save-and-close id]])}
     "Save"]]])

(defn cmdk-item
  [{:keys [label action icon] :as attrs}]
  (when-not (= (:type attrs) :separator)
    [:> Command/CommandItem
     {:on-select #(rf/dispatch [::dialog.events/close action])}
     [:div.w-7.h-7.mr-2.rounded.line-height-6.flex.justify-center.items-center
      {:class (when icon "overlay")}
      (when icon [views/icon icon])]
     label
     [:div.right-slot
      [views/shortcuts action]]]))

(defn cmdk-group-inner
  [items label]
  (for [i items]
    (if (:items i)
      (cmdk-group-inner (:items i) (:label i))
      ^{:key (:id i)}
      [cmdk-item (update i :label #(string/join " - " (remove nil? [label %])))])))

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
    {:placeholder (t [:cmdk/search-command "Search for a command"])}]
   [views/scroll-area
    [:> Command/CommandList
     {:class "p-1"}
     [:> Command/CommandEmpty
      (t [:cmdk/no-results "No results found."])]
     (for [i (menubar.views/submenus)]
       ^{:key (:id i)}
       [cmdk-group i])]]])

(defn root
  []
  (let [dialogs @(rf/subscribe [::dialog.subs/entities])]
    [:> Dialog/Root
     {:open (seq dialogs)
      :on-open-change #(rf/dispatch [::dialog.events/close])}
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
       (when (:close-button (last dialogs))
         [:> Dialog/Close
          {:class "icon-button absolute top-5 right-5 small"
           :aria-label "Close"}
          [views/icon "times"]])
       [:> Dialog/Description
        {:as-child true}
        [:div (:content (last dialogs))]]]]]))
