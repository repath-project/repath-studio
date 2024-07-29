(ns renderer.dialog.cmdk
  (:require
   ["cmdk" :as Command]
   [clojure.string :as str]
   [i18n :refer [t]]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.menubar.views :as menubar]))

(defn item
  [{:keys [label action key icon type]}]
  (when-not (= type :separator)
    [:> Command/CommandItem
     {:key key
      :on-select (fn []
                   (rf/dispatch [::dialog.e/close false])
                   (rf/dispatch action))}
     [:div.w-7.h-7.mr-2.rounded.line-height-6.flex.justify-center.items-center
      {:class (when icon "overlay")}
      (when icon [comp/icon icon {:class "icon"}])]
     label
     [:div.right-slot
      [comp/shortcuts action]]]))

(defn group-inner
  [items label]
  (for [i items]
    ^{:key key}
    (if-not (:items i)
      [item (update i :label #(str/join " - " (remove nil? [label %])))]
      (group-inner (:items i) (:label i)))))

(defn group
  [{:keys [label items]}]
  [:> Command/CommandGroup
   {:heading label}
   (group-inner items nil)])

(defn root
  []
  [:> Command/Command
   {:label "Command Menu"
    :on-key-down #(when-not (= (.-key %) "Escape") (.stopPropagation %))}
   [:> Command/CommandInput
    {:placeholder (t [:cmdk/search-command "Search for a command"])}]
   [:> Command/CommandList
    [:> Command/CommandEmpty
     (t [:cmdk/no-results "No results found."])]
    (for [i (menubar/root-menu)]
      ^{:key (:key i)}
      [group i])]])
