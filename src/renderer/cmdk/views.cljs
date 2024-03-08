(ns renderer.cmdk.views
  (:require
   ["cmdk" :as Command]
   [i18n :refer [t]]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.menubar.views :as menubar]))

(defn group-item
  [{:keys [label action key icon type]}]
  (if (= type :separator)
    [:> Command/CommandSeparator]
    [:> Command/CommandItem
     {:key key
      :on-select (fn []
                   (rf/dispatch action)
                   (rf/dispatch [:cmdk/set false]))}
     [:div.w-7.h-7.mr-2.rounded.line-height-6.flex.justify-center.items-center
      {:class (when icon "overlay")}
      (when icon [comp/icon icon {:class "icon"}])]
     label
     [:div.right-slot
      [comp/shortcuts action]]]))

(defn group
  [{:keys [label items key]}]
  [:> Command/CommandGroup
   {:heading label}
   (for [item items]
     ^{:key key}
     (if (:items item)
       [group item]
       [group-item item]))])

(defn dialog
  []
  [:> Command/CommandDialog
   {:open @(rf/subscribe [:cmdk/visible?])
    :onOpenChange #(rf/dispatch [:cmdk/set %])
    :label (t [:cmdk/command-palette "Command palette"])
    :on-key-down #(when-not (= (.-key %) "Escape") (.stopPropagation %))
    :class "dialog"}
   [:> Command/CommandInput
    {:placeholder (t [:cmdk/search-command "Search for a command"])}]
   [:> Command/CommandList
    [:> Command/CommandEmpty
     (t [:cmdk/no-results "No results found."])]
    (map group (menubar/root-menu))]])
