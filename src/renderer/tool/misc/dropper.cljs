(ns renderer.tool.misc.dropper
  (:require
   [re-frame.core :as rf]
   [renderer.app.handlers :as app.h]
   [renderer.color.effects :as-alias color.fx]
   [renderer.document.handlers :as document.h]
   [renderer.element.events :as-alias element.e]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :refer [finalize]]
   [renderer.notification.events :as-alias notification.e]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :dropper ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :dropper
  []
  {:icon "eye-dropper"
   :description "Pick a color from your document."})

(defmethod tool.hierarchy/help [:dropper :default]
  []
  "Click anywhere to pick a color.")

(defmethod tool.hierarchy/activate :dropper
  [db]
  (if (.-EyeDropper js/window)
    (app.h/add-fx db [::color.fx/dropper {:on-success ::success
                                          :on-error ::error}])
    (-> db
        (app.h/set-tool :select)
        (notification.h/add
         [notification.v/unavailable-feature
          "EyeDropper"
          "https://developer.mozilla.org/en-US/docs/Web/API/EyeDropper_API#browser_compatibility"]))))

(rf/reg-event-db
 ::success
 [(finalize "Pick color")]
 (fn [db [_ ^js color]]
   (let [srgb (.-sRGBHex color)]
     (-> db
         (document.h/assoc-attr :fill srgb)
         (element.h/assoc-attr :fill srgb)
         (app.h/set-tool :select)))))

(rf/reg-event-db
 ::error
 (fn [db [_ error]]
   (-> db
       (app.h/set-tool :select)
       (notification.h/add
        [:div
         [:h2.pb-4.font-bold "EyeDropper canceled"]
         [:div.text-error (str error)]]))))
