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

(def FocusType [:enum :original :fit :fill])

(def Viewbox
  [:tuple
   [number? {:title "x"}]
   [number? {:title "y"}]
   [number? {:title "width"}]
   [number? {:title "height"}]])
