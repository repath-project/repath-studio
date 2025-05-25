(ns renderer.ruler.db)

(def Ruler
  [:map {:closed true}
   [:visible {:default true} boolean?]
   [:locked {:default false} boolean?]])

(def Orientation
  [:enum :vertical :horizontal])
