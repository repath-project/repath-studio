(ns renderer.dialog.db)

(def dialog
  [:map
   [:title {:optional true} string?]
   [:content {:optional true} vector?]
   [:attrs {:optional true} map?]])
