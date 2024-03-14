(ns renderer.dialog.save
  (:require
   [re-frame.core :as rf]
   [renderer.dialog.views :as v]))

(defn root
  [k]
  (let [document @(rf/subscribe [:document/document k])]
    [:div.p-4
     [:h1.text-xl.mb-2
      "Do you want to save your changes?"]
     [:p "Your changes to " [:strong (:title document)] " will be lost if you close the document without saving."]
     [:div.flex.gap-2
      [:button.button.px-2.bg-primary.rounded.flex-1
       {:on-click #(do (rf/dispatch [:dialog/close])
                       (rf/dispatch [:document/close k false]))}
       "Don't save"]
      [:button.button.px-2.bg-primary.rounded.flex-1
       {:on-click #(rf/dispatch [:dialog/close])}
       "Cancel"]
      [:button.button.px-2.bg-primary.rounded.flex-1
       {:on-click #(do (rf/dispatch [:dialog/close])
                       (rf/dispatch [:document/save-and-close]))}
       "Save"]]
     [v/close]]))
