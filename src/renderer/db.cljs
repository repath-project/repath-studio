(ns renderer.db
  (:require
   [renderer.dialog.db :as dialog.db]
   [renderer.document.db :as document.db]
   [renderer.snap.db :as snap.db]
   [renderer.theme.db :as theme.db]
   [renderer.timeline.db :as timeline.db]
   [renderer.window.db :as window.db]))

(def app
  [:map
   [:tool [keyword? {:default :select}]]
   [:pointer-pos [:tuple {:default [0 0]} double? double?]]
   [:zoom-sensitivity [:and {:default 0.75} double? pos?]]
   [:state [keyword? {:default :default}]]
   [:grid-visible? [boolean? {:default false}]]
   [:rulers-visible? [boolean? {:default true}]]
   [:snap snap.db/snap]
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
   [:panel [:map-of {:default {:tree {:visible? true}
                               :properties {:visible? true}
                               :timeline {:visible? false}
                               :xml {:visible? false}
                               :repl-history {:visible? false}}}
            keyword? [:map [:visible? boolean?]]]]])
