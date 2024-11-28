(ns renderer.tool.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.e]
   [renderer.utils.dom :as dom]))

(rf/reg-fx
 ::set-pointer-capture
 (fn [pointer-id]
   (.setPointerCapture (dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::release-pointer-capture
 (fn [pointer-id]
   (.releasePointerCapture (dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::drop
 (fn [[position data-transfer]]
   (doseq [item (.-items data-transfer)]
     (when (= (.-kind item) "string")
       (let [[x y] position]
         (.getAsString item #(rf/dispatch [::element.e/add {:type :element
                                                            :tag :text
                                                            :content %
                                                            :attrs {:x x
                                                                    :y y}}])))))

   (doseq [file (.-files data-transfer)]
     (rf/dispatch [::element.e/import-file file position]))))
