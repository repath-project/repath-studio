(ns repath.studio.tools.dropper
  (:require [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            ["html2canvas" :as html2canvas]))

(derive :dropper ::tools/edit)

(defmethod tools/properties :dropper [] {:icon "eye-dropper"})

(defmethod tools/activate :dropper
  [db]
  (js/window.api.send "toMain" #js {:action "beginFrameSubscription"})
  (assoc db :cursor "crosshair"))

(defmethod tools/deactivate :dropper
  [db]
  (js/window.api.send "toMain" #js {:action "endFrameSubscription"})
  db)

(defmethod tools/click :dropper
  [db event element]
  (let [{:keys [mouse-pos]} event
        body (-> (js/document.getElementById "canvas-frame") (.-contentWindow) (.-document) (.-body))
        html-canvas (html2canvas body)]
    (.then html-canvas #(rf/dispatch [:set-fill (-> % (.getContext "2d") (.getImageData (first mouse-pos) (second mouse-pos) 1 1) (.-data))]))))
