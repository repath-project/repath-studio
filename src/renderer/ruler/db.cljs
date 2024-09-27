(ns renderer.ruler.db)

(def Ruler
  [:map {:default {} :closed true}
   [:visible {:default true} boolean?]
   [:locked {:default false} boolean?]
   [:size {:default 23} number?]])

(def Direction
  [:enum :vertical :horizontal])
