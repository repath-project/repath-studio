(ns renderer.menubar.db)

(def Menu [:enum :file :edit :object :view :help])

(def Menubar
  [:map {:closed true}
   [:indicator {:optional true} boolean?]
   [:active {:optional true} Menu]])
