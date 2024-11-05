(ns renderer.worker.effects
  (:require
   [re-frame.core :as rf]
   [renderer.worker.events :as-alias worker.e]))

(rf/reg-fx
 ::post
 (fn [{:keys [data on-success]}]
   (let [worker (js/Worker. "js/worker.js")]
     (.addEventListener
      worker
      "message"
      #(let [response-data (js->clj (.. % -data) :keywordize-keys true)
             id (uuid (:id response-data))]
         (rf/dispatch [::worker.e/completed id on-success response-data])))
     (.postMessage worker (clj->js data)))))
