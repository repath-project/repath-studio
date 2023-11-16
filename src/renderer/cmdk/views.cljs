(ns renderer.cmdk.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.window.views :as window]
   [i18n :refer [t]]
   ["cmdk" :as Command]))

(defn command-dialog
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
    (map (fn [{:keys [label items key]}]
           ^{:key key}
           [:> Command/CommandGroup
            [:div.px-3.py-2.text-muted.uppercase.font-bold
             {:style {:font-size "10px"}}
             label]
            (map (fn [{:keys [label action key]}]
                   (when action
                     ^{:key key}
                     [:> Command/CommandItem
                      {:key key
                       :on-select #(doall (rf/dispatch action)
                                          (rf/dispatch [:cmdk/set false]))}
                      label
                      [:div {:class "right-slot"}
                       [comp/shortcuts action]]]))
                 items)])
         (window/menu))]])