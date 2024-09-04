(ns renderer.app.db
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.dialog.db :refer [Dialog]]
   [renderer.document.db :refer [Document]]
   [renderer.element.db :refer [Element Handle]]
   [renderer.snap.db :refer [Snap]]
   [renderer.theme.db :refer [Theme]]
   [renderer.timeline.db :refer [Timeline]]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.bounds :refer [Bounds]]
   [renderer.utils.hiccup :refer [Hiccup]]
   [renderer.utils.math :refer [Vec2D]]
   [renderer.window.db :refer [Window]]))

(def Panels
  [:map-of {:default {:tree {:visible? true}
                      :properties {:visible? true}
                      :timeline {:visible? false}
                      :xml {:visible? false}
                      :repl-history {:visible? false}}}
   keyword? [:map [:visible? boolean?]]])

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

(def Tool
  [:fn {:error/fn (fn [{:keys [value]} _] (str value ", is not a supported tool"))}
   tool.hierarchy/tool?])

(def App
  [:map {:closed true}
   [:tool {:default :select} Tool]
   [:primary-tool {:optional true} Tool]
   [:pointer-pos {:default [0 0]} Vec2D]
   [:pointer-offset {:optional true} Vec2D]
   [:adjusted-pointer-pos {:default [0 0]} Vec2D]
   [:adjusted-pointer-offset {:optional true} Vec2D]
   [:drag? {:optional true} boolean?]
   [:zoom-sensitivity {:default 0.75} [:and number? pos?]]
   [:state {:default :default} keyword?]
   [:grid-visible? {:default false :persisted true} boolean?]
   [:rulers-visible? {:default true :persisted true} boolean?]
   [:snap {:persisted true} Snap]
   [:active-document {:optional true :persisted true} [:maybe uuid?]]
   [:cursor {:default "default"} string?]
   [:dom-rect {:optional true} DomRect]
   [:rulers-locked? {:default false} boolean?]
   [:dialogs {:default []} [:vector Dialog]]
   [:documents {:default {} :persisted true} [:map-of uuid? Document]]
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
   [:window Window]
   [:theme {:persisted true} Theme]
   [:timeline Timeline]
   [:panels {:persisted true} Panels]
   [:version {:optional true :persisted true} string?]
   [:fx {:default []} vector?]
   [:pivot-point {:optional true} Vec2D]
   [:clicked-element {:optional true} [:or Element Handle]]
   [:copied-bounds {:optional true} Bounds]
   [:copied-elements {:optional true} [:cat Element]]
   [:mdn {:optional true} map?]
   [:webref-css {:optional true} map?]
   [:message {:optional true} Hiccup]
   [:re-pressed.core/keydown {:optional true} any?]])

(def valid? (m/validator App))

(def explain (m/explainer App))

(def default (m/decode App {:version config/version} mt/default-value-transformer))

(def persistent-keys
  "Top level keys that should be persisted to lcoal storage."
  (->> App
       (m/children)
       (filter (fn [[_key props]] (:persisted props)))
       (map first)))


