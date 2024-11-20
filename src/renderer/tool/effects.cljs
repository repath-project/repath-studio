(ns renderer.tool.effects
  (:require
   [re-frame.core :as rf]
   [renderer.utils.dom :as dom]
   [renderer.utils.drop :as drop]))

(rf/reg-fx
 ::set-pointer-capture
 (fn [pointer-id]
   (.setPointerCapture (dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::release-pointer-capture
 (fn [pointer-id]
   (.releasePointerCapture (dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::data-transfer
 (fn [[position data-transfer]]
   (drop/items! position (.-items data-transfer))
   (drop/files! position (.-files data-transfer))))
