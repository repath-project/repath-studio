(ns renderer.cmdk.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.window.views :as window]
   [i18n :refer [t]]
   ["cmdk" :refer [CommandDialog
                   CommandGroup
                   CommandList
                   CommandItem
                   CommandInput
                   CommandEmpty]]))

(defn command-dialog
  []
  [:> CommandDialog
   {:open @(rf/subscribe [:cmdk/visible?])
    :onOpenChange #(rf/dispatch [:cmdk/set %])
    :label (t [:cmdk/command-menu "Command menu"])
    :class "dialog"}
   [:> CommandInput
    {:placeholder (t [:cmdk/search-command "Search for a command"])}]
   [:> CommandList
    [:> CommandEmpty
     (t [:cmdk/no-results "No results found."])]
    (map (fn [{:keys [label items key]}]
           ^{:key key}
           [:> CommandGroup
            [:div.px-3.py-2.text-muted.uppercase.font-bold
             {:style {:font-size "10px"}}
             label]
            (map (fn [{:keys [label action key]}]
                   (when action
                     ^{:key key}
                     [:> CommandItem
                      {:key key
                       :on-select #(doall (rf/dispatch action)
                                          (rf/dispatch [:cmdk/set false]))}
                      label
                      [:div {:class "right-slot"}
                       [comp/shortcuts action]]]))
                 items)])
         (window/menu))]])