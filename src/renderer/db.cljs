(ns renderer.db
  (:require
   [renderer.dialog.db :as dialog.db]
   [renderer.document.db :as document.db]
   [renderer.snap.db :as snap.db]
   [renderer.theme.db :as theme.db]
   [renderer.timeline.db :as timeline.db]
   [renderer.tool.base :as tool]
   [renderer.utils.math :as math]
   [renderer.window.db :as window.db]))

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

(def tool
  [:fn {:error/fn (fn [{:keys [value]} _] (str value ", is not a supported tool"))}
   tool/tool?])

(def app
  [:map
   [:tool {:default :select} tool]
   [:pointer-pos math/point]
   [:adjusted-pointer-pos math/point]
   [:adjusted-pointer-offset math/point]
   [:zoom-sensitivity {:default 0.75} [:and number? pos?]]
   [:state {:default :default} keyword?]
   [:grid-visible? {:default false} boolean?]
   [:rulers-visible? {:default true} boolean?]
   [:snap snap.db/snap]
   [:active-document {:optional true} [:maybe keyword?]]
   [:cursor {:default "default"} string?]
   [:dom-rect {:optional true} dom-rect]
   [:rulers-locked? {:default false} boolean?]
   [:dialogs {:default []} [:vector dialog.db/dialog]]
   [:documents {:default {}} [:map-of keyword? document.db/document]]
   [:document-tabs {:default []} [:vector keyword?]]
   [:recent {:max 10 :default []} [:vector string?]]
   [:system-fonts {:optional true} vector?]
   [:notifications {:default []} vector?]
   [:debug-info? {:default false} boolean?]
   [:pen-mode? {:default false} boolean?]
   [:backdrop? {:default false} boolean?]
   [:lang {:default :en-US} keyword?]
   [:repl-mode {:default :cljs} keyword?]
   [:worker {:default {:tasks {}}} [:map [:tasks map?]]]
   [:window window.db/window]
   [:theme theme.db/theme]
   [:timeline timeline.db/timeline]
   [:panels panels]
   [:version {:optional true} string?]
   [:fx {:default []} vector?]])

