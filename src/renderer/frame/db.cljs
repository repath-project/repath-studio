(ns renderer.frame.db)

(def DomRect
  [:map {:closed true}
   [:x number?]
   [:y number?]
   [:width number?]
   [:height number?]
   [:top number?]
   [:right number?]
   [:bottom number?]
   [:left number?]])
