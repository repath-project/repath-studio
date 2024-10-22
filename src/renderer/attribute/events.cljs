(ns renderer.attribute.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.element.events :as-alias element.e]))

;; TODO: Debounce persist.
(rf/reg-event-fx
 ::update-and-focus
 (fn [_ [_ k f & more]]
   {:fx [[:dispatch (apply vector ::element.e/update-attr k f more)]
         [:dispatch ^:flush-dom [::app.e/focus (name k)]]]}))
