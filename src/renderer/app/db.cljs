(ns renderer.app.db
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.transform :as m.transform]
   [renderer.dialog.db :refer [Dialog]]
   [renderer.document.db :refer [Document]]
   [renderer.element.db :refer [Element]]
   [renderer.frame.db :refer [DomRect]]
   [renderer.notification.db :refer [Notification]]
   [renderer.ruler.db :refer [Ruler]]
   [renderer.snap.db :refer [Snap NearestNeighbor]]
   [renderer.theme.db :refer [Theme]]
   [renderer.timeline.db :refer [Timeline]]
   [renderer.tool.db :refer [Handle Tool State Cursor]]
   [renderer.utils.bounds :refer [BBox]]
   [renderer.utils.i18n :as utils.i18n]
   [renderer.utils.math :refer [Vec2]]
   [renderer.window.db :refer [Window]]))

(def Panels
  [:map-of keyword? [:map [:visible boolean?]]])

(def Lang
  [:fn {:error/fn (fn [{:keys [value]} _] (str value " is not a supported language"))}
   utils.i18n/supported-lang?])

(def Platform
  [:enum "darwin" "linux" "win32" "web"])

(def Font
  [:map-of {:title "style"} string? [:map
                                     [:postscript-name string?]
                                     [:full-name string?]]])

(def App
  [:map {:closed true}
   [:tool {:default :transform} Tool]
   [:primary-tool {:optional true} Tool]
   [:pointer-pos {:default [0 0]} Vec2]
   [:pointer-offset {:optional true} Vec2]
   [:adjusted-pointer-pos {:default [0 0]} Vec2]
   [:adjusted-pointer-offset {:optional true} Vec2]
   [:nearest-neighbor-offset {:optional true} [:maybe Vec2]]
   [:nearest-neighbor {:optional true} [:maybe NearestNeighbor]]
   [:nearest-neighbors {:optional true} [:sequential NearestNeighbor]]
   [:drag {:optional true} boolean?]
   [:zoom-sensitivity {:default 0.75} [:and number? pos?]]
   [:event-timestamp {:optional true} number?]
   [:double-click-delta {:default 250} [:and number? pos?]]
   [:state {:default :idle} State]
   [:grid {:default false :persist true} boolean?]
   [:ruler {:default {} :persist true} Ruler]
   [:snap {:default {} :persist true} Snap]
   [:active-document {:optional true :persist true} [:maybe uuid?]]
   [:cursor {:default "default"} Cursor]
   [:dom-rect {:optional true} DomRect]
   [:dialogs {:default []} [:vector Dialog]]
   [:documents {:default {} :persist true} [:map-of uuid? Document]]
   [:document-tabs {:default [] :persist true} [:vector uuid?]]
   [:recent {:max 10 :default [] :persist true} [:vector string?]]
   [:drag-threshold {:default 1} number?]
   [:system-fonts {:optional true} [:map-of string? Font]]
   [:notifications {:default []} [:* Notification]]
   [:debug-info {:default false} boolean?]
   [:help-bar {:default true} boolean?]
   [:pen-mode {:default false} boolean?]
   [:backdrop {:default false} boolean?]
   [:lang {:optional true :persist true} Lang]
   [:system-lang {:optional true} string?]
   [:platform {:optional true} Platform]
   [:versions {:optional true} [:maybe map?]]
   [:env {:optional true} [:maybe map?]]
   [:user-agent {:optional true} string?]
   [:repl-mode {:default :cljs} keyword?]
   [:worker {:default {:tasks {}}} [:map [:tasks map?]]]
   [:window {:default {}} Window]
   [:theme {:default {} :persist true} Theme]
   [:timeline {:default {}} Timeline]
   [:panels {:default {:tree {:visible true}
                       :properties {:visible true}
                       :timeline {:visible false}
                       :xml {:visible false}
                       :history {:visible false}
                       :repl-history {:visible false}} :persist true} Panels]
   [:version {:optional true :persist true} string?]
   [:fx {:default []} vector?]
   [:pivot-point {:optional true} Vec2]
   [:clicked-element {:optional true} [:or Element Handle]]
   [:copied-bbox {:optional true} BBox]
   [:copied-elements {:optional true} [:* Element]]
   [:kdtree {:optional true} [:maybe map?]]
   [:viewbox-kdtree {:optional true} [:maybe map?]]
   [:re-pressed.core/keydown {:optional true} map?]])

(def valid? (m/validator App))

(def explain (m/explainer App))

(def default
  (m/decode App {:version config/version} m.transform/default-value-transformer))

(def persisted-keys
  "Top level keys that should be persisted to local storage."
  (->> App
       (m/children)
       (filter (comp :persist second))
       (map first)))
