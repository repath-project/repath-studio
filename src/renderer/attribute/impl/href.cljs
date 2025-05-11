(ns renderer.attribute.impl.href
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/href"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.events :as app.events]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.notification.events :as-alias notification.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.ui :as ui]))

(defmethod attribute.hierarchy/description [:default :href]
  []
  "The href attribute defines a link to a resource as a reference URL.
   The exact meaning of that link depends on the context of each element using it.")

(defmethod attribute.hierarchy/form-element [:default :href]
  [_ k v {:keys [disabled]}]
  (let [state-default? (= @(rf/subscribe [::tool.subs/state]) :idle)
        data-url? (and v (string/starts-with? v "data:"))]
    [:div.flex.gap-px.w-full
     [attribute.views/form-input k (if data-url? "data-url" v)
      {:placeholder (when-not v "multiple")
       :disabled (or disabled
                     data-url?
                     (not v)
                     (not state-default?))}]
     [:button.form-control-button
      {:title "Select file"
       :disabled disabled
       :on-click #(rf/dispatch
                   [::app.events/file-open {:options {:startIn "pictures"
                                                      :types [{:accept {"image/png" [".png"]
                                                                        "image/jpeg" [".jpeg" ".jpg"]
                                                                        "image/bmp" [".fmp"]}}]}
                                            :on-success [::success]}])}
      [ui/icon "folder"]]]))

(rf/reg-event-fx
 ::success
 (fn [{:keys [db]} [_ file]]
   {:db (tool.handlers/activate db :transform)
    ::app.effects/file-read-as [file
                                :data-url
                                {"load" {:on-fire [::element.events/set-attr :href]}
                                 "error" {:on-fire [::notification.events/exception]}}]}))
