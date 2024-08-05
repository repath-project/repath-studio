(ns renderer.timeline.effects
  (:require
   [re-frame.core :as rf]
   [renderer.utils.dom :as dom]))

(rf/reg-fx
 ::set-current-time
 (fn [time]
   (doall (map #(.setCurrentTime % time) (dom/svg-elements)))))

(rf/reg-fx
 ::pause-animations
 (fn []
   (doall (map #(.pauseAnimations %) (dom/svg-elements)))))
