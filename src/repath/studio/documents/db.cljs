(ns repath.studio.documents.db
  (:require [repath.studio.elements.db :as elements]
            [cljs.spec.alpha :as s]))

(s/def ::hovered-keys set?)
(s/def ::selected-keys set?)
(s/def ::active-page keyword?)
(s/def ::rulers-locked? boolean?)
(s/def ::zoom number?)
(s/def ::pan (s/coll-of number? :kind vector?))
(s/def ::elements (s/and (s/map-of keyword? ::elements/element)))

(s/def ::document
  (s/keys :req-un [::hovered-keys ::selected-keys ::pages ::active-page ::rulers-locked? ::zoom ::elements ::pan]))

(def default-document
  {:hovered-keys (hash-set)
   :selected-keys (hash-set)
   :active-page :default-page
   :rulers-locked? false
   :grid? false
   :rulers? true
   :fill [255 255 255 1]
   :stroke [0 0 0 1]
   :stroke-width 1
   :zoom 1
   :rotate 0
   :filter :no-filter
   :pan [0 0]
   :attrs {:fill "#ffffff"}
   :elements {:canvas {:key :canvas
                       :visible? true
                       :type :canvas
                       :attrs {:fill "#eeeeee"}
                       :children [:default-page]}
              :default-page {:key :default-page
                             :name "Page"
                             :visible? true
                             :type :page
                             :parent :canvas
                             :attrs {:width 800
                                     :height 600
                                     :x 0
                                     :y 0
                                     :fill "#ffffff"}
                             :children []}}})
