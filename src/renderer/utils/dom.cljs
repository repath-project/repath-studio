(ns renderer.utils.dom)

(def DomElement
  [:fn (fn [x] (instance? js/Element x))])

(defn prevent-default!
  [e]
  (.preventDefault e))

(defn stop-propagation!
  [e]
  (.stopPropagation e))

(defn frame-document!
  []
  (when-let [frame (.getElementById js/document "frame")]
    (when-let [window (.-contentWindow frame)]
      (.-document window))))

(defn svg-elements!
  []
  (when-let [document (frame-document!)]
    (.querySelectorAll document "svg")))

(defn canvas-element!
  []
  (when-let [document (frame-document!)]
    (.getElementById document "canvas")))
