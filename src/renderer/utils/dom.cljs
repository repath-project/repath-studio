(ns renderer.utils.dom)

(def DomElement
  [:fn (fn [x] (instance? js/Element x))])

(defn frame-document!
  []
  (when-let [frame (.getElementById js/document "frame")]
    (when-let [window (.-contentWindow frame)]
      (.-document window))))

(defn canvas-element!
  []
  (when-let [document (frame-document!)]
    (.getElementById document "canvas")))
