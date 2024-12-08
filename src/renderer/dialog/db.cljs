(ns renderer.dialog.db
  (:require
   [renderer.utils.hiccup :refer [Hiccup]]))

(def Dialog
  [:map {:closed true}
   [:title {:optional true} Hiccup]
   [:content {:optional true} any?]
   [:close-button {:optional true} boolean?]
   [:attrs {:optional true} [:map [:as-child {:optional true :default false} boolean?]
                             [:force-mount {:optional true} boolean?]
                             [:on-open-auto-focus {:optional true} ifn?]
                             [:on-close-auto-focus {:optional true} ifn?]
                             [:on-escape-key-down {:optional true} ifn?]
                             [:on-pointer-down-outside {:optional true} ifn?]
                             [:on-interact-outside {:optional true} ifn?]]]])
