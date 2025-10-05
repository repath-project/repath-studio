(ns renderer.dialog.db)

(def Dialog
  [:map {:closed true}
   [:title {:optional true} any?]
   [:content {:optional true} any?]
   [:has-close-button {:optional true} boolean?]
   [:attrs {:optional true} [:map [:as-child {:optional true} boolean?]
                             [:force-mount {:optional true} boolean?]
                             [:on-open-auto-focus {:optional true} ifn?]
                             [:on-close-auto-focus {:optional true} ifn?]
                             [:on-escape-key-down {:optional true} ifn?]
                             [:on-pointer-down-outside {:optional true} ifn?]
                             [:on-interact-outside {:optional true} ifn?]]]])
