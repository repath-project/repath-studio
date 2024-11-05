(ns renderer.frame.subs
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.frame.handlers :as h]))

(rf/reg-sub
 ::viewbox
 :<- [::document.s/zoom]
 :<- [::document.s/pan]
 :<- [::app.s/dom-rect]
 (fn [[zoom pan dom-rect] _]
   (h/viewbox zoom pan dom-rect)))

(rf/reg-sub
 ::viewbox-attr
 :<- [::viewbox]
 (fn [viewbox _]
   (str/join " " viewbox)))
