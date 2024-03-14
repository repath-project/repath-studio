(ns renderer.dialog.views
  (:require
   ["@radix-ui/react-dialog" :as Dialog]
   [re-frame.core :as rf]
   [renderer.components :as comp]))

(defn close
  []
  [:> Dialog/Close
   {:class "dialog-close small"
    :aria-label "Close"}
   [comp/icon "times"]])

(defn root
  []
  (let [dialog @(rf/subscribe [:dialog])]
    [:> Dialog/Root
     {:open dialog
      :on-open-change #(rf/dispatch [:dialog/close])}
     [:> Dialog/Portal
      [:> Dialog/Overlay {:class "dialog-overlay"}]
      [:> Dialog/Content {:class "dialog-content"}
       (:content dialog)]]]))
