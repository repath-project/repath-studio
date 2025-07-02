(ns renderer.tool.impl.misc.dropper
  (:require
   [re-frame.core :as rf]
   [renderer.document.handlers :as document.handlers]
   [renderer.effects :as effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.notification.handlers :as notification.handlers]
   [renderer.notification.views :as notification.views]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.i18n :refer [t]]))

(derive :dropper ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :dropper
  []
  {:icon "eye-dropper"})

(defmethod tool.hierarchy/help [:dropper :idle]
  []
  (t [::help "Click anywhere to pick a color."]))

(defmethod tool.hierarchy/on-activate :dropper
  [db]
  (if (.-EyeDropper js/window)
    (tool.handlers/add-fx db [::effects/eye-dropper {:on-success [::success]
                                                     :on-error [::error]}])
    (-> db
        (tool.handlers/activate :transform)
        (notification.handlers/add
         (notification.views/unavailable-feature
          "EyeDropper"
          "https://developer.mozilla.org/en-US/docs/Web/API/EyeDropper_API#browser_compatibility")))))

(rf/reg-event-db
 ::success
 (fn [db [_ ^js color]]
   (let [srgb-color (.-sRGBHex color)]
     (-> db
         (document.handlers/assoc-attr :fill srgb-color)
         (element.handlers/assoc-attr :fill srgb-color)
         (history.handlers/finalize "Pick color")
         (tool.handlers/activate :transform)))))

(rf/reg-event-db
 ::error
 (fn [db [_ error]]
   (-> db
       (tool.handlers/activate :transform)
       (notification.handlers/add (notification.views/exception error)))))
