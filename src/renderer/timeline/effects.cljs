(ns renderer.timeline.effects
  (:require
   [re-frame.core :as rf]
   [renderer.utils.dom :as dom]))

(defn svg-elements!
  []
  (when-let [document (dom/frame-document!)]
    (.querySelectorAll document "svg")))

(rf/reg-fx
 ::set-current-time
 (fn [t]
   (doall (map #(.setCurrentTime % t) (svg-elements!)))))

(rf/reg-fx
 ::pause-animations
 (fn []
   (doall (map #(.pauseAnimations %) (svg-elements!)))))
