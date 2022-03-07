(ns repath.studio.tools.dropper
  (:require [repath.studio.tools.base :as tools]
            [clojure.core.matrix :as matrix]
            [repath.studio.documents.handlers :as documents]
            [repath.studio.history.handlers :as history]
            [goog.color :as gcolor]))

(derive :dropper ::tools/edit)

(defmethod tools/properties :dropper [] {:icon "eye-dropper"})

(defmethod tools/activate :dropper
  [db]
  (js/window.api.send "toMain" #js {:action "beginFrameSubscription"})
  (assoc db :cursor "crosshair"))

(defmethod tools/deactivate :dropper
  [db]
  (js/window.api.send "toMain" #js {:action "endFrameSubscription"})
  (dissoc db :overlay))

(defn get-pixel-color
  [bitmap bitmap-width x y]
  (let [b-index (+ (* y (* 4 bitmap-width))
                   (* x 4))
        b (aget bitmap b-index)
        g (aget bitmap (+ b-index 1))
        r (aget bitmap (+ b-index 2))
        a (aget bitmap (+ b-index 3))]
    [r g b a]))

(defmethod tools/click :dropper
  [{:keys [mouse-pos content-rect] :as db}]
  (let [bitmap (:window/bitmap db)
        size (:window/size db)
        [mouse-x mouse-y] (matrix/add mouse-pos [(:x content-rect) (:y content-rect)])
        color (get-pixel-color bitmap (:width size) mouse-x mouse-y)]
    (-> db
        (documents/set-fill color)
        (history/finalize (str "Pick color " (tools/rgba color))))))

(defmethod tools/mouse-move :dropper
  [{:keys [mouse-pos content-rect] :as db}]
  (let [bitmap (:window/bitmap db)
        size (:window/size db)
        [x y] (matrix/add mouse-pos [(:x content-rect) (:y content-rect)])
        color (get-pixel-color bitmap (:width size) x y)]
    (assoc db :overlay [:div {:style {:display "flex"
                                    }}
                        #_[:div {:style {:width "100px"
                                         :height "100px"
                                         :display "block"
                                         :background-position (str (- x 50) "px " (- y 50) "px")
                                         :background-image (str "url(" png ")")}}]
                        [:div {:style {:width "24px"
                                       :height "24px"
                                       :background-color (tools/rgba color)}}]
                        [:span {:style {:line-height "24px" :height "24px" :display "inline-block" :margin-left "12px"}} (gcolor/rgbArrayToHex (clj->js color))]])))



