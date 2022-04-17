(ns repath.studio.db
  (:require [repath.studio.documents.db :as documents]
            [cljs.spec.alpha :as s]))

(s/def ::tool keyword?)
(s/def ::state #{:default :select :create :translate :scale :clone :edit})
(s/def ::stroke-width number?)
(s/def ::mouse-pos (s/map-of number? number?))
(s/def ::active-theme keyword?)
(s/def ::active-document keyword?)
(s/def ::document-tabs (s/coll-of keyword? :kind vector? :distinct true))
(s/def ::system-fonts  (s/coll-of string? :kind vector? :distinct true))
(s/def ::max-undos number?)
(s/def ::documents (s/and (s/map-of keyword? ::documents/document)))
(s/def ::db (s/keys :req-un [::tool ::state ::stroke-width ::mouse-pos ::active-theme ::active-document ::document-tabs ::system-fonts ::max-undos ::window ::documents]))

(def default-db
  {:tool :select
   :mouse-pos [0 0]
   :zoom-factor 0.8
   :state :default
   :active-theme :dark
   :documents {}
   :document-tabs []
   :system-fonts []
   :max-undos 100
   :debug-info? false
   :window {:maximized? true
            :minimized? false
            :fullscreen? false
            :tree? true
            :properties? true
            :header? true
            :history? false
            :timeline? true
            :xml? false
            :left-sidebar-width 300
            :right-sidebar-width 300
            :elements-collapsed? false
            :pages-collapsed? false
            :command-palette? false
            :defs-collapsed? true
            :symbols-collapsed? true
            :repl-history-collapsed? true}
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
