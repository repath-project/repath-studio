(ns renderer.document.effects
  (:require
   ["localforage" :as localforage]
   [config :as config]
   [re-frame.core :as rf]))

(defn- find-same-entry
  [ks file-handle]
  (->> ks
       (remove #(= % config/app-name))
       (map #(vector % (localforage/getItem %)))
       (map (fn [[id promise]]
              (-> promise
                  (.then (fn [handle]
                           (some-> handle
                                   (.isSameEntry file-handle)
                                   (.then #(when %
                                             (uuid id)))))))))))

(rf/reg-fx
 ::query-file-handle
 (fn [{:keys [file-handle on-found on-not-found on-error]}]
   (-> (localforage/keys)
       (.then (fn [ks]
                (-> (find-same-entry ks file-handle)
                    (js/Promise.all)
                    (.then #(if-let [open-id (->> (remove nil? %)
                                                  (first))]
                              (some-> on-found (conj open-id) rf/dispatch)
                              (some-> on-not-found rf/dispatch)))
                    (.catch #(some-> on-error (conj %) rf/dispatch))))))))
