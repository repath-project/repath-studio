(ns repath.studio.window.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::left-sidebar map)
(s/def ::right-sidebar map)
(s/def ::collapsed? boolean?)
(s/def ::size number?)
(s/def ::header? boolean?)
(s/def ::history? boolean?)
(s/def ::rulers? boolean?)
(s/def ::elements-collapsed? boolean?)
(s/def ::pages-collapsed? boolean?)
(s/def ::defs-collapsed? boolean?)
(s/def ::symbols-collapsed? boolean?)
(s/def ::repl-history-collapsed? boolean?)
(s/def ::maximized? boolean?)
(s/def ::minimized? boolean?)

(s/def ::window (s/keys :req-un [::left-sidebar
                                 ::right-sidebar
                                 ::header?
                                 ::history?
                                 ::rulers?
                                 ::elements-collapsed?
                                 ::pages-collapsed?
                                 ::defs-collapsed?
                                 ::symbols-collapsed?
                                 ::repl-history-collapsed?
                                 ::maximized?
                                 ::minimized?]))