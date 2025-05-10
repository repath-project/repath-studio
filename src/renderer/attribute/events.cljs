(ns renderer.attribute.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.element.events :as-alias element.events]))

;; TODO: Debounce persist.
(rf/reg-event-fx
 ::update-and-focus
 (fn [_ [_ k f & more]]
   {:fx [[:dispatch (apply vector ::element.events/update-attr k f more)]
         [:dispatch ^:flush-dom [::app.events/focus (name k)]]]}))
