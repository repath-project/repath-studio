(ns renderer.ruler.db)

(def Ruler
  [:map {:closed true}
   [:visible {:default true} boolean?]
   [:locked {:default false} boolean?]
   [:size {:default 23} number?]])

(def Orientation
  [:enum :vertical :horizontal])
