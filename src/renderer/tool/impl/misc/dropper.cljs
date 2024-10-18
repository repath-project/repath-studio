(ns renderer.tool.impl.misc.dropper
  (:require
   [re-frame.core :as rf]
   [renderer.app.handlers :as app.h]
   [renderer.color.effects :as-alias color.fx]
   [renderer.document.handlers :as document.h]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :refer [finalize]]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :dropper ::hierarchy/tool)

(defmethod hierarchy/properties :dropper
  []
  {:icon "eye-dropper"})

(defmethod hierarchy/help [:dropper :idle]
  []
  "Click anywhere to pick a color.")

(defmethod hierarchy/activate :dropper
  [db]
  (if (.-EyeDropper js/window)
    (app.h/add-fx db [::color.fx/dropper {:on-success ::success
                                          :on-error ::error}])
    (-> db
        (app.h/set-tool :transform)
        (notification.h/add
         (notification.v/unavailable-feature
          "EyeDropper"
          "https://developer.mozilla.org/en-US/docs/Web/API/EyeDropper_API#browser_compatibility")))))

(rf/reg-event-db
 ::success
 [(finalize "Pick color")]
 (fn [db [_ ^js color]]
   (let [srgb (.-sRGBHex color)]
     (-> db
         (document.h/assoc-attr :fill srgb)
         (element.h/assoc-attr :fill srgb)
         (app.h/set-tool :transform)))))

(rf/reg-event-db
 ::error
 (fn [db [_ error]]
   (-> db
       (app.h/set-tool :transform)
       (notification.h/add (notification.v/exception error)))))
