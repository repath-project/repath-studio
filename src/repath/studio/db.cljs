(ns repath.studio.db
  (:require [repath.studio.documents.db :as documents]
            [cljs.spec.alpha :as s]))

(s/def ::tool keyword?)
(s/def ::state #{:default :select :create :move :scale :clone :edit})
(s/def ::stroke-width number?)
(s/def ::mouse-pos (s/map-of number? number?))
(s/def ::left-sidebar-width number?)
(s/def ::right-sidebar-width number?)
(s/def ::tree? boolean?)
(s/def ::properties? boolean?)
(s/def ::header? boolean?)
(s/def ::history? boolean?)
(s/def ::rulers? boolean?)
(s/def ::elements-collapsed? boolean?)
(s/def ::pages-collapsed? boolean?)
(s/def ::defs-collapsed? boolean?)
(s/def ::symbols-collapsed? boolean?)
(s/def ::active-theme keyword?)
(s/def ::active-document keyword?)
(s/def ::document-tabs (s/coll-of keyword? :kind vector? :distinct true))
(s/def ::system-fonts  (s/coll-of string? :kind vector? :distinct true))
(s/def ::max-undos number?)
(s/def ::maximized? boolean?)
(s/def ::minimized? boolean?)
(s/def ::mouse-over-canvas? boolean?)
(s/def ::documents (s/and (s/map-of keyword? ::documents/document)))

(s/def ::db (s/keys :req-un [::tool ::stroke-width ::mouse-pos ::left-sidebar-width  ::right-sidebar-width ::tree? ::properties? ::header? ::history?  ::rulers? ::elements-collapsed? ::pages-collapsed? ::defs-collapsed? ::symbols-collapsed? ::active-theme ::active-document ::document-tabs ::system-fonts ::max-undos ::maximized? ::minimized? ::mouse-over-canvas?]))

(def default-db
  {:tool :select
   :mouse-pos [0 0]
   :left-sidebar-width 300
   :right-sidebar-width 300
   :zoom-factor 0.8
   :tree? true
   :properties? true
   :header? true
   :history? false
   :xml-view? false
   :state :default
   :elements-collapsed? false
   :pages-collapsed? false
   :command-palette? false
   :defs-collapsed? true
   :symbols-collapsed? true
   :active-theme :dark
   :documents {}
   :document-tabs []
   :system-fonts []
   :max-undos 100
   :window/maximized? true
   :window/minimized? false
   :window/fullscreen? false
   :debug-info? false
   :mouse-over-canvas? false
   :color-palette [[[0 0 128 1]
                    [0 0 255 1]
                    [0 128 0 1]
                    [0 255 0 1]
                    [0 128 128 1]
                    [0 255 255 1]
                    [128 0 0 1]
                    [255 0 0 1]
                    [128 0 128 1]
                    [255 0 255 1]
                    [128 128 0 1]
                    [255 255 0 1]]
                   [[0 0 0 0]
                    [0 0 0 1]
                    [26 26 26 1]
                    [51 51 51 1]
                    [77 77 77 1]
                    [102 102 102 1]
                    [128 128 128 1]
                    [153 153 153 1]
                    [179 179 179 1]
                    [204 204 204 1]
                    [230 230 230 1]
                    [255 255 255 1]]]})
