(ns renderer.utils.dom
  (:require
   [malli.core :as m]
   [renderer.db :refer [JS_Object]]))

(defn frame-document!
  []
  (when-let [frame (.getElementById js/document "frame")]
    (when-let [window (.-contentWindow frame)]
      (.-document window))))

(defn canvas-element!
  []
  (when-let [document (frame-document!)]
    (.getElementById document "canvas")))

(m/=> event->uuid [:-> JS_Object [:maybe uuid?]])
(defn event->uuid
  [e]
  (-> (.-dataTransfer e)
      (.getData "id")
      (uuid)))
