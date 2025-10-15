(ns renderer.dialog.views
  (:require
   ["@radix-ui/react-dialog" :as Dialog]
   ["cmdk" :as Command]
   [clojure.string :as string]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.dialog.subs :as-alias dialog.subs]
   [renderer.document.events :as-alias document.events]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]
   [renderer.window.menubar :as window.menubar.views]))

(defn button
  [{:keys [action label auto-focus class]}]
  [:button.button.px-4.rounded.flex-1.font-medium
   {:class class
    :auto-focus auto-focus
    :on-click #(rf/dispatch [::dialog.events/close action])}
   (or label (t [::cancel "Cancel"]))])

(defn button-bar
  [& children]
  (into [:div.flex.gap-2] children))

(defn about
  []
  (let [user-agent @(rf/subscribe [::app.subs/user-agent])]
    [:div.p-5
     [:div.flex.gap-3.items-start.pb-2
      [:p
       [:span.block [:strong (t [::version "Version:"])] config/version]
       [:span.block [:strong (t [::browser "Browser:"])] user-agent]]]
     [button-bar
      [button {:label (t [::ok "OK"])
               :auto-focus true
               :class "accent"}]]]))

(defn confirmation
  [{:keys [description confirm-action confirm-label cancel-action
           cancel-label]}]
  [:div.p-5
   (cond->> description
     (string? description)
     (into [:p]))
   [button-bar
    [button {:label (or cancel-label (t [::cancel "Cancel"]))
             :action cancel-action}]
    [button {:label (or confirm-label (t [::ok "OK"]))
             :action confirm-action
             :auto-focus true
             :class "accent"}]]])

(defn save
  [{:keys [id title]}]
  [:div.p-5
   (t [::changes-will-be-lost
       [:p "Your changes to %1 will be lost if you close the document without
            saving."]]
      [[:strong title]])
   [button-bar
    [button {:label (t [::dont-save "Don't save"])
             :action [::document.events/close id false]}]
    [button {:label (t [::cancel "Cancel"])}]
    [button {:label (t [::save "Save"])
             :auto-focus true
             :class "accent"
             :action [::document.events/save {:id id
                                              :close true}]}]]])

(defn cmdk-item
  [{:keys [label action icon disabled]
    :as attrs}]
  (when-not (or (= (:type attrs) :separator)
                disabled)
    [:> Command/CommandItem
     {:on-select #(rf/dispatch [::dialog.events/close action])}
     [:div.flex.items-center.gap-2
      [:div.w-7.h-7.rounded.line-height-6.flex.justify-center.items-center
       {:class (when icon "bg-overlay")}
       (when icon [views/icon icon])]
      [:div label]]
     [views/shortcuts action]]))

(defn cmdk-group-inner
  [items label]
  (for [item items]
    (if (:items item)
      (cmdk-group-inner (:items item) (:label item))
      ^{:key (:id item)}
      [cmdk-item (update item :label #(->> [label %]
                                           (remove nil?)
                                           (string/join " - ")))])))

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
    {:class "p-3 bg-primary border-b border-border w-full"
     :placeholder (t [::search-command "Search for a command"])}]
   [views/scroll-area
    [:> Command/CommandList
     {:class "p-1"}
     [:> Command/CommandEmpty
      (t [::no-results "No results found."])]
     (for [menu (window.menubar.views/submenus)]
       ^{:key (:id menu)}
       [cmdk-group menu])]]])

(defn root
  []
  (let [active-dialog @(rf/subscribe [::dialog.subs/active])
        {:keys [title content attrs]} active-dialog]
    [:> Dialog/Root
     {:open (boolean active-dialog)
      :on-open-change #(rf/dispatch [::dialog.events/close])}
     [:> Dialog/Portal
      [:> Dialog/Overlay {:class "backdrop"}]
      [:> Dialog/Content
       (views/merge-with-class
        {:class "fixed bg-primary rounded-lg overflow-hidden shadow-xl border
                 border-border left-1/2 top-1/2 w-125 max-w-9/10 -translate-1/2
                 animate-in zoom-in-95"
         :on-key-down #(.stopPropagation %)}
        attrs)
       (when title
         [:> Dialog/Title
          {:as-child true}
          (if (string? title)
            [:h2.text-xl.px-5.pt-5.text-foreground-hovered title]
            title)])
       [:> Dialog/Description
        {:as-child true}
        [:div content]]]]]))
