(ns renderer.color.events
  (:require
   [re-frame.core :as rf]
   [renderer.document.handlers :as document.h]
   [renderer.element.handlers :as element.h]))

(rf/reg-event-db
 ::preview
 (fn [db [_ k v]]
   (-> db
       (document.h/assoc-attr k v)
       (element.h/set-attr k v))))
