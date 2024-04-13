(ns renderer.menubar.events
  (:require
   [re-frame.core :as rf]
   [renderer.utils.dom :as dom]))

(rf/reg-fx
 ::focus
 (fn [id]
   (when-let [element (if id (.getElementById js/document id) (dom/canvas-element))]
     (js/setTimeout #(.focus element)))))

(rf/reg-event-fx
 :menubar/focus
 (fn [_ [_ id]]
   {::focus id}))
