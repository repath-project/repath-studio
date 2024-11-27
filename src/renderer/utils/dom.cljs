(ns renderer.utils.dom)

(defn frame-document!
  []
  (when-let [frame (.getElementById js/document "frame")]
    (when-let [window (.-contentWindow frame)]
      (.-document window))))

(defn canvas-element!
  []
  (when-let [document (frame-document!)]
    (.getElementById document "canvas")))
