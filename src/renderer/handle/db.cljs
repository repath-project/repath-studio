(ns renderer.handle.db)

(def HandleAction
  [:enum :translate :scale :edit])

(def Handle
  [:map {:closed true}
   [:id keyword?]
   [:label {:optional true} string?]
   [:action HandleAction]
   [:type [:= :handle]]
   [:cursor {:optional true} string?]
   [:x {:optional true} number?]
   [:y {:optional true} number?]
   [:size {:optional true} number?]
   [:stroke-width {:optional true} number?]
   [:element {:optional true} uuid?]])
