(ns repath.studio.tools.dropper
  (:require [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]))

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
  [db event _]
  (let [bitmap (:window/bitmap db)
        bitmap-width (:width (:window/bitmap-size db))
        {:keys [mouse-pos]} event
        canvas-frame (-> (.getElementById js/document "canvas-frame") (.getBoundingClientRect))
        canvas-frame-pos [(.-x canvas-frame) (.-y canvas-frame)]
        mouse-x (+ (first mouse-pos) (first canvas-frame-pos))
        mouse-y (+ (second mouse-pos) (second canvas-frame-pos))]
    (rf/dispatch [:set-fill (get-pixel-color bitmap bitmap-width mouse-x mouse-y)]))
  db)




