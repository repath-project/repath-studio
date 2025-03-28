(ns renderer.tool.impl.misc.dropper
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as app.fx]
   [renderer.document.handlers :as document.h]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :dropper ::hierarchy/tool)

(defmethod hierarchy/properties :dropper
  []
  {:icon "eye-dropper"})

(defmethod hierarchy/help [:dropper :idle]
  []
  "Click anywhere to pick a color.")

(defmethod hierarchy/on-activate :dropper
  [db]
  (if (.-EyeDropper js/window)
    (h/add-fx db [::app.fx/eye-dropper {:on-success [::success]
                                        :on-error [::error]}])
    (-> (h/activate db :transform)
        (notification.h/add
         (notification.v/unavailable-feature
          "EyeDropper"
          "https://developer.mozilla.org/en-US/docs/Web/API/EyeDropper_API#browser_compatibility")))))

(rf/reg-event-db
 ::success
 (fn [db [_ ^js color]]
   (let [srgb-color (.-sRGBHex color)]
     (-> (document.h/assoc-attr db :fill srgb-color)
         (element.h/assoc-attr :fill srgb-color)
         (h/activate :transform)
         (history.h/finalize "Pick color")))))

(rf/reg-event-db
 ::error
 (fn [db [_ error]]
   (-> (h/activate db :transform)
       (notification.h/add (notification.v/exception error)))))
