(ns renderer.utils.dom)

(defn root-element
  []
  (.getElementById js/document "app"))

(defn canvas-element
  []
  (when-let [frame (.getElementById js/document "frame")]
    (.getElementById (.. frame -contentWindow -document) "canvas")))
