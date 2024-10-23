(ns renderer.app.db
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.dialog.db :refer [Dialog]]
   [renderer.document.db :refer [Document]]
   [renderer.element.db :refer [Element]]
   [renderer.frame.db :refer [DomRect]]
   [renderer.handle.db :refer [Handle]]
   [renderer.notification.db :refer [Notification]]
   [renderer.ruler.db :refer [Ruler]]
   [renderer.snap.db :refer [Snap]]
   [renderer.theme.db :refer [Theme]]
   [renderer.timeline.db :refer [Timeline]]
   [renderer.tool.db :refer [Tool State Cursor]]
   [renderer.utils.bounds :refer [Bounds]]
   [renderer.utils.i18n :as i18n]
   [renderer.utils.math :refer [Vec2D]]
   [renderer.window.db :refer [Window]]))

(def Panels
  [:map-of {:default {:tree {:visible true}
                      :properties {:visible true}
                      :timeline {:visible false}
                      :xml {:visible false}
                      :history {:visible false}
                      :repl-history {:visible false}}}
   keyword? [:map [:visible boolean?]]])

(def Lang
  [:fn {:error/fn (fn [{:keys [value]} _] (str value " is not a supported language"))}
   i18n/lang?])

(def App
  [:map {:closed true}
   [:tool {:default :transform} Tool]
   [:primary-tool {:optional true} Tool]
   [:pointer-pos {:default [0 0]} Vec2D]
   [:pointer-offset {:optional true} Vec2D]
   [:adjusted-pointer-pos {:default [0 0]} Vec2D]
   [:adjusted-pointer-offset {:optional true} Vec2D]
   [:drag {:optional true} boolean?]
   [:zoom-sensitivity {:default 0.75} [:and number? pos?]]
   [:event-time {:optional true} number?]
   [:double-click-delta {:default 250} [:and number? pos?]]
   [:state {:default :idle} State]
   [:grid {:default false :persist true} boolean?]
   [:ruler {:persist true} Ruler]
   [:snap {:persist true} Snap]
   [:active-document {:optional true :persist true} [:maybe uuid?]]
   [:cursor {:default "default"} Cursor]
   [:dom-rect {:optional true} DomRect]
   [:dialogs {:default []} [:vector Dialog]]
   [:documents {:default {} :persist true} [:map-of uuid? Document]]
   [:document-tabs {:default [] :persist true} [:vector uuid?]]
   [:recent {:max 10 :default [] :persist true} [:vector string?]]
   [:drag-threshold {:default 1} number?]
   [:system-fonts {:optional true} vector?]
   [:notifications {:default []} [:* Notification]]
   [:debug-info {:default false} boolean?]
   [:pen-mode {:default false} boolean?]
   [:backdrop {:default false} boolean?]
   [:explanation {:optional true} string?]
   [:lang {:default :en-Us :persist true} Lang]
   [:repl-mode {:default :cljs} keyword?]
   [:worker {:default {:tasks {}}} [:map [:tasks map?]]]
   [:window Window]
   [:theme {:persist true} Theme]
   [:timeline Timeline]
   [:panels {:persist true} Panels]
   [:version {:optional true :persist true} string?]
   [:fx {:default []} vector?]
   [:pivot-point {:optional true} Vec2D]
   [:clicked-element {:optional true} [:or Element Handle]]
   [:copied-bounds {:optional true} Bounds]
   [:copied-elements {:optional true} [:* Element]]
   [:re-pressed.core/keydown {:optional true} map?]])

(def valid? (m/validator App))

(def explain (m/explainer App))

(def default (m/decode App {:version config/version} mt/default-value-transformer))

(def persistent-keys
  "Top level keys that should be persisted to local storage."
  (->> App
       (m/children)
       (filter (comp :persist second))
       (map first)))
