(ns worker.triangulate
  (:require
   ["triangulate-image" :as triangulate]
   [promesa.core :as p]))

(def options
  #js {:accuracy 0.5 ; float between 0 and 1
       :blur 40 ; positive integer
       :threshold 50 ; integer between 1 and 100
       :vertexCount 100 ; positive integer
       :fill true ; boolean or string with css color (e.g '#bada55', 'red', rgba(100,100,100,0.5))
       :stroke true ; boolean or string with css color (e.g '#bada55', 'red', hsla(0, 50%, 52%, 0.5))
       :strokeWidth 0.5 ; positive float
       :gradients false ; boolean
       :gradientStops 4 ; positive integer >= 2
       :lineJoin " miter " ; 'miter', 'round', or 'bevel'
       :transparentColor false ; boolean false or string with css color
       })

(defn init []
  (js/self.addEventListener
   "message"
   (fn [^js e]
     (p/let [data (.-data e)
             svg (-> options
                     (triangulate)
                     (.fromImageDataSync (.-image data))
                     (.toSVG))]
       (js/postMessage #js {:svg svg
                            :name (str "Triangulated " (or (.-name data) "image"))
                            :position (.-position data)
                            :id (.-id data)})))))
