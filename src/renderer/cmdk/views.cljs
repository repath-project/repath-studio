(ns renderer.cmdk.views
  (:require
   ["cmdk" :as Command]
   [i18n :refer [t]]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.menubar.views :as menubar]))

(defn item
  [group {:keys [label action key type]}]
  ^{:key key}
  (when-not (= type :separator)
    [:> Command/CommandItem
     {:key key
      :on-select (fn []
                   (rf/dispatch action)
                   (rf/dispatch [:cmdk/set false]))}
     (str group " / " label)
     [:div.right-slot
      [comp/shortcuts action]]]))

(defn group
  [{:keys [label items key]}]
  ^{:key key}
  ;; TODO: recur groups
  [:> Command/CommandGroup
   #_[:div.px-3.py-2.text-muted.uppercase.font-bold
      {:style {:font-size "10px"}}
      label]
   (map #(if (:items %)
           (map (fn [i] (item (str label " / " (:label %)) i)) (:items %))
           (item label %))
        items)])

(defn dialog
  []
  [:> Command/CommandDialog
   {:open @(rf/subscribe [:cmdk/visible?])
    :onOpenChange #(rf/dispatch [:cmdk/set %])
    :label (t [:cmdk/command-palette "Command palette"])
    :class "dialog"}
   [:> Command/CommandInput
    {:placeholder (t [:cmdk/search-command "Search for a command"])
     :on-key-down #(.stopPropagation %)}]
   [:> Command/CommandList
    [:> Command/CommandEmpty
     (t [:cmdk/no-results "No results found."])]
    (map group menubar/menu)]])
