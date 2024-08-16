(ns renderer.db
  (:require
   [malli.core :as m]
   [renderer.dialog.db :as dialog.db]
   [renderer.document.db :as document.db]
   [renderer.snap.db :as snap.db]
   [renderer.theme.db :as theme.db]
   [renderer.timeline.db :as timeline.db]
   [renderer.tool.base :as tool]
   [renderer.window.db :as window.db]))

(def point
  [:tuple {:default [0 0]} number? number?])

(def panels
  [:map-of {:default {:tree {:visible? true}
                      :properties {:visible? true}
                      :timeline {:visible? false}
                      :xml {:visible? false}
                      :repl-history {:visible? false}}}
   keyword? [:map [:visible? boolean?]]])

(def dom-rect
 [:map {:closed true}
  [:x number?]
  [:y number?]
  [:width number?]
  [:height number?]
  [:top number?]
  [:right number?]
  [:bottom number?]
  [:left number?]])

(def app
  [:map
   [:tool [:fn {:default :select} tool/valid?]]
   [:pointer-pos point]
   [:adjusted-pointer-pos point]
   [:adjusted-pointer-offset point]
   [:zoom-sensitivity [:and {:default 0.75} double? pos?]]
   [:state [keyword? {:default :default}]]
   [:grid-visible? [boolean? {:default false}]]
   [:rulers-visible? [boolean? {:default true}]]
   [:snap snap.db/snap]
   [:restored? {:optional true} boolean?]
   [:active-document {:optional true} keyword?]
   [:cursor [string? {:default "default"}]]
   [:dom-rect {:optional true} dom-rect]
   [:rulers-locked? [boolean? {:default false}]]
   [:dialogs [:vector {:default []} dialog.db/dialog]]
   [:documents [:map-of {:default {}} keyword? document.db/document]]
   [:document-tabs [:vector {:default []} keyword?]]
   [:recent [:vector {:max 10 :default []} string?]]
   [:system-fonts {:optional true} vector?]
   [:notifications [vector? {:default []}]]
   [:debug-info? [boolean? {:default false}]]
   [:pen-mode? [boolean? {:default false}]]
   [:backdrop? [boolean? {:default false}]]
   [:lang [keyword? {:default :en-US}]]
   [:repl-mode [keyword? {:default :cljs}]]
   [:worker [:map {:default {:tasks {}}} [:tasks map?]]]
   [:window window.db/window]
   [:theme theme.db/theme]
   [:timeline timeline.db/timeline]
   [:panels panels]])

(def valid? (m/validator app))
