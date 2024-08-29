(ns worker.trace
  (:require
   ["imagetracerjs" :as ImageTracer]))

(defn ^:export init! []
  (js/self.addEventListener
   "message"
   (fn [^js e]
     (let [data (.-data e)
           svg (.imagedataToSVG ImageTracer (.-image data))]
       (js/postMessage #js {:svg svg
                            :name (str "Traced " (or (.-name data) "image"))
                            :position (.-position data)
                            :id (.-id data)})))))
