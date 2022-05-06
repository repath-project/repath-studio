(ns repath.studio.tools.dropper
  (:require [repath.studio.tools.base :as tools]
            [clojure.core.matrix :as matrix]
            [repath.studio.styles :as styles]
            [repath.studio.documents.handlers :as documents]
            [repath.studio.history.handlers :as history]
            [goog.color :as gcolor]))

(derive :dropper ::tools/misc)

(defmethod tools/properties :dropper [] {:icon "eye-dropper"
                                         :description "Pick a color from your document."})

(defmethod tools/activate :dropper
  [db]
  (js/window.api.send "toMain" #js {:action "beginFrameSubscription"})
  (assoc db :cursor "crosshair"))

(defmethod tools/deactivate :dropper
  [db]
  (js/window.api.send "toMain" #js {:action "endFrameSubscription"})
  (dissoc db :overlay))

(defn get-pixel-color
  [bitmap bitmap-width [x y]]
  (let [b-index (+ (* y (* 4 bitmap-width))
                   (* x 4))
        b (aget bitmap b-index)
        g (aget bitmap (+ b-index 1))
        r (aget bitmap (+ b-index 2))
        a (aget bitmap (+ b-index 3))]
    [r g b a]))

(defmethod tools/mouse-up :dropper
  [{:keys [mouse-pos content-rect] :as db}]
  (let [position (matrix/add mouse-pos [(:x content-rect) (:y content-rect)])
        color (get-pixel-color (-> db :window :bitmap) (-> db :window :size :width) position)]
    (-> db
        (documents/set-fill color)
        (history/finalize (str "Pick color " (tools/rgba color))))))

(defn color-block
  [color size active?]
  [:div {:style {:width size
                 :height size
                 :box-sizing "border-box"
                 :border (str "1px solid " (if active? styles/accent "hsla(0, 0%, 50%, .2)"))
                 :background-color color}}])

(defmethod tools/mouse-move :dropper
  [{:keys [mouse-pos content-rect] :as db}]
  (let [bitmap (-> db :window :bitmap)
        bitmap-width (-> db :window :size :width)
        position (matrix/add mouse-pos [(:x content-rect) (:y content-rect)])
        color (get-pixel-color bitmap bitmap-width position)
        grid-size 11
        block-size 10
        center-position 5
        container-size (* grid-size  block-size)]
    (assoc db :overlay [:div 
                        [:div {:style {:width container-size :height container-size :display "flex" :flex-wrap "wrap"}}
                         (map (fn [offset-y]
                                (map (fn [offset-x]
                                       (let [position (matrix/sub (matrix/add position [offset-x offset-y]) center-position)
                                             color (gcolor/rgbArrayToHex (clj->js (get-pixel-color bitmap bitmap-width position)))
                                             active? (and (= offset-x center-position) (= offset-y center-position))]
                                        [color-block color block-size active?])) (range grid-size))) (range grid-size))]
                        [:div {:style {:display "flex" :padding-top "12px"}}
                         [:div {:style {:width "24px" :height "24px" :background-color (tools/rgba color)}}]
                         [:span {:style {:line-height "24px" :margin-left "12px"}} (gcolor/rgbArrayToHex (clj->js color))]]])))



