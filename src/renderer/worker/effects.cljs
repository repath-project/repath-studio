(ns renderer.worker.effects
  (:require
   [re-frame.core :as rf]))

(rf/reg-fx
 ::post
 (fn [{:keys [worker data callback]}]
   (let [worker (js/Worker. (str "js/" worker))]
     (.. worker (addEventListener "message" callback))
     (.postMessage worker (clj->js data)))))
