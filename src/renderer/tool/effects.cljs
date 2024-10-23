(ns renderer.tool.effects
  (:require
   [re-frame.core :as rf]
   [renderer.utils.dom :as dom]))

(rf/reg-fx
 ::set-pointer-capture
 (fn [pointer-id]
   (.setPointerCapture (dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::release-pointer-capture
 (fn [pointer-id]
   (.releasePointerCapture (dom/canvas-element!) pointer-id)))

(def custom-fx
  (rf/->interceptor
   :id ::custom-fx
   :after (fn [context]
            (let [db (rf/get-effect context :db ::not-found)]
              (cond-> context
                (not= db ::not-found)
                (-> (rf/assoc-effect :fx (apply conj (or (:fx (rf/get-effect context)) []) (:fx db)))
                    (rf/assoc-effect :db (assoc db :fx []))))))))

(rf/reg-global-interceptor custom-fx)
