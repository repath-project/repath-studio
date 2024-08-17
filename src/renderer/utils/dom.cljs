(ns renderer.utils.dom)

(defn root-element
  []
  (.getElementById js/document "app"))

(defn frame-element
  []
  (.getElementById js/document "frame"))

(defn frame-window
  []
  (when-let [frame (frame-element)]
    (.-contentWindow frame )))

(defn frame-document
  []
  (when-let [window (frame-window)]
    (.-document window)))

(defn focused?
  []
  (or (.hasFocus js/document)
      (when-let [document (frame-document)]
        (.hasFocus document))))

(defn svg-elements
  []
  (when-let [document (frame-document)]
    (.querySelectorAll document "svg")))

(defn canvas-element
  []
  (when-let [document (frame-document)]
    (.getElementById document "canvas")))

(defn scroll-into-view!
  [el]
  (.scrollIntoView el #js {:block "nearest"}))

(defn scroll-to-bottom!
  [el]
  (set! (.-scrollTop el) (.-scrollHeight el)))
