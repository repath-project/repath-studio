(ns renderer.utils.dom)

(defn root-element
  []
  (.getElementById js/document "app"))

(defn frame-element
  []
  (.getElementById js/document "frame"))

(defn canvas-document
  []
  (when-let [frame (frame-element)]
    (.. frame -contentWindow -document)))

(defn svg-elements
  []
  (when-let [document (canvas-document)]
    (.querySelectorAll document "svg")))

(defn canvas-element
  []
  (when-let [document (canvas-document)]
    (.getElementById document "canvas")))

(defn scroll-into-view
  [el]
  (.scrollIntoView el #js {:block "nearest"}))
