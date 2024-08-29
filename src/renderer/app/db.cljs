(ns renderer.app.db
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.dialog.db :as dialog.db]
   [renderer.document.db :as document.db]
   [renderer.element.db :as element.db]
   [renderer.snap.db :as snap.db]
   [renderer.theme.db :as theme.db]
   [renderer.timeline.db :as timeline.db]
   [renderer.tool.base :as tool]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.hiccup :as hiccup]
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
  [:map {:closed true}
   [:tool {:default :select} tool]
   [:primary-tool {:optional true} tool]
   [:pointer-pos {:default [0 0]} math/point]
   [:pointer-offset {:optional true} math/point]
   [:adjusted-pointer-pos {:default [0 0]} math/point]
   [:adjusted-pointer-offset {:optional true} math/point]
   [:drag? {:optional true} boolean?]
   [:zoom-sensitivity {:default 0.75} [:and number? pos?]]
   [:state {:default :default} keyword?]
   [:grid-visible? {:default false :persisted true} boolean?]
   [:rulers-visible? {:default true :persisted true} boolean?]
   [:snap snap.db/snap]
   [:active-document {:optional true :persisted true} [:maybe keyword?]]
   [:cursor {:default "default"} string?]
   [:dom-rect {:optional true} dom-rect]
   [:rulers-locked? {:default false} boolean?]
   [:dialogs {:default []} [:vector dialog.db/dialog]]
   [:documents {:default {} :persisted true} [:map-of keyword? document.db/document]]
   [:document-tabs {:default [] :persisted true} [:vector keyword?]]
   [:recent {:max 10 :default [] :persisted true} [:vector string?]]
   [:drag-threshold {:default 1} number?]
   [:system-fonts {:optional true} vector?]
   [:notifications {:default []} vector?]
   [:debug-info? {:default false} boolean?]
   [:pen-mode? {:default false} boolean?]
   [:backdrop? {:default false} boolean?]
   [:lang {:default :en-US} keyword?]
   [:repl-mode {:default :cljs} keyword?]
   [:worker {:default {:tasks {}}} [:map [:tasks map?]]]
   [:window window.db/window]
   [:theme {:persisted true} theme.db/theme]
   [:timeline timeline.db/timeline]
   [:panels {:persisted true} panels]
   [:version {:optional true :persisted true} string?]
   [:fx {:default []} vector?]
   [:pivot-point {:optional true} math/point]
   [:clicked-element {:optional true} [:or element.db/element element.db/handle]]
   [:copied-bounds {:optional true} bounds/bounds]
   [:copied-elements {:optional true} [:cat element.db/element]]
   [:mdn {:optional true} map?]
   [:webref-css {:optional true} map?]
   [:message {:optional true} hiccup/hiccup]
   [:re-pressed.core/keydown any?]])

(def valid? (m/validator app))

(def default (m/decode app {:version config/version} mt/default-value-transformer))

(def persistent-keys
  "Top level keys that should be persisted"
  (->> app
       m/children
       (filter (fn [[_key props]] (:persisted props)))
       (map first)))


