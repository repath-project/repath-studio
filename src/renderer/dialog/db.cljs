(ns renderer.dialog.db
  (:require
   [renderer.utils.hiccup :as hiccup]))

(def dialog
  [:map
   [:title {:optional true} hiccup/hiccup]
   [:content {:optional true} any?]
   [:close-button? {:optional true} boolean?]
   [:attrs {:optional true} [:map [:as-child {:optional true :default false} boolean?]
                                  [:force-mount {:optional true} boolean?]
                                  [:on-open-auto-focus {:optional true} fn?]
                                  [:on-close-auto-focus {:optional true} fn?]
                                  [:on-escape-key-down {:optional true} fn?]
                                  [:on-pointer-down-outside {:optional true} fn?]
                                  [:on-interact-outside {:optional true} fn?]]]])
