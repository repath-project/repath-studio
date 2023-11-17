(ns renderer.cmdk.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.menu.bar :as menu.bar]
   [i18n :refer [t]]
   ["cmdk" :as Command]))

(defn item
  [{:keys [label action key type]}]
  ^{:key key}
  (when-not (= type :separator)
   [:> Command/CommandItem
    {:key key
     :on-select #(doall (rf/dispatch action)
                        (rf/dispatch [:cmdk/set false]))}
    (when (= type :checkbox) "Toggle > ") label
    [:div.right-slot
     [comp/shortcuts action]]]))

(defn group
  [{:keys [label items key]}]
  ^{:key key}
  [:> Command/CommandGroup
   [:div.px-3.py-2.text-muted.uppercase.font-bold
    {:style {:font-size "10px"}}
    label]
   (map #(if (:items %)
           (map item (:items %))
           (item %))
        items)])

(defn dialog
  []
  [:> Command/CommandDialog
   {:open @(rf/subscribe [:cmdk/visible?])
    :onOpenChange #(rf/dispatch [:cmdk/set %])
    :label (t [:cmdk/command-menu "Command menu"])
    :class "dialog"}
   [:> Command/CommandInput
    {:placeholder (t [:cmdk/search-command "Search for a command"])}]
   [:> Command/CommandList
    [:> Command/CommandEmpty
     (t [:cmdk/no-results "No results found."])]
    (map group menu.bar/menu)]])