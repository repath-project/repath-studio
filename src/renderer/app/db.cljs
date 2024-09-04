(ns renderer.app.db
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.dialog.db :refer [dialog]]
   [renderer.document.db :refer [document]]
   [renderer.element.db :refer [element handle]]
   [renderer.snap.db :refer [snap]]
   [renderer.theme.db :refer [theme]]
   [renderer.timeline.db :refer [timeline]]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.bounds :refer [bounds]]
   [renderer.utils.hiccup :refer [hiccup]]
   [renderer.utils.math :refer [vec2d]]
   [renderer.window.db :refer [window]]))

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
   tool.hierarchy/tool?])

(def app
  [:map {:closed true}
   [:tool {:default :select} tool]
   [:primary-tool {:optional true} tool]
   [:pointer-pos {:default [0 0]} vec2d]
   [:pointer-offset {:optional true} vec2d]
   [:adjusted-pointer-pos {:default [0 0]} vec2d]
   [:adjusted-pointer-offset {:optional true} vec2d]
   [:drag? {:optional true} boolean?]
   [:zoom-sensitivity {:default 0.75} [:and number? pos?]]
   [:state {:default :default} keyword?]
   [:grid-visible? {:default false :persisted true} boolean?]
   [:rulers-visible? {:default true :persisted true} boolean?]
   [:snap {:persisted true} snap]
   [:active-document {:optional true :persisted true} [:maybe uuid?]]
   [:cursor {:default "default"} string?]
   [:dom-rect {:optional true} dom-rect]
   [:rulers-locked? {:default false} boolean?]
   [:dialogs {:default []} [:vector dialog]]
   [:documents {:default {} :persisted true} [:map-of uuid? document]]
   [:document-tabs {:default [] :persisted true} [:vector uuid?]]
   [:recent {:max 10 :default [] :persisted true} [:vector string?]]
   [:drag-threshold {:default 1} number?]
   [:ruler-size {:default 23} number?]
   [:system-fonts {:optional true} vector?]
   [:notifications {:default []} vector?]
   [:debug-info? {:default false} boolean?]
   [:pen-mode? {:default false} boolean?]
   [:backdrop? {:default false} boolean?]
   [:loading? {:default false} boolean?]
   [:explanation {:optional true} string?]
   [:lang {:default :en-US :persited true} keyword?]
   [:repl-mode {:default :cljs} keyword?]
   [:worker {:default {:tasks {}}} [:map [:tasks map?]]]
   [:window window]
   [:theme {:persisted true} theme]
   [:timeline timeline]
   [:panels {:persisted true} panels]
   [:version {:optional true :persisted true} string?]
   [:fx {:default []} vector?]
   [:pivot-point {:optional true} vec2d]
   [:clicked-element {:optional true} [:or element handle]]
   [:copied-bounds {:optional true} bounds]
   [:copied-elements {:optional true} [:cat element]]
   [:mdn {:optional true} map?]
   [:webref-css {:optional true} map?]
   [:message {:optional true} hiccup]
   [:re-pressed.core/keydown {:optional true} any?]])

(def valid? (m/validator app))

(def explain (m/explainer app))

(def default (m/decode app {:version config/version} mt/default-value-transformer))

(def persistent-keys
  "Top level keys that should be persisted to lcoal storage."
  (->> app
       (m/children)
       (filter (fn [[_key props]] (:persisted props)))
       (map first)))


