(ns renderer.menubar.db)

(def Menubar
  [:map {:closed true}
   [:indicator {:optional true} boolean?]
   [:active {:optional true} keyword?]])
