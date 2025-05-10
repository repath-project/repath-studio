(ns renderer.tool.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]
   [renderer.utils.dom :as utils.dom]))

(rf/reg-fx
 ::set-pointer-capture
 (fn [pointer-id]
   (.setPointerCapture (utils.dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::release-pointer-capture
 (fn [pointer-id]
   (.releasePointerCapture (utils.dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::drop
 (fn [[position data-transfer]]
   (doseq [item (.-items data-transfer)]
     (when (= (.-kind item) "string")
       (let [[x y] position]
         (.getAsString item #(rf/dispatch [::element.events/add {:type :element
                                                                 :tag :text
                                                                 :content %
                                                                 :attrs {:x x
                                                                         :y y}}])))))

   (doseq [file (.-files data-transfer)]
     (rf/dispatch [::element.events/import-file file position]))))
