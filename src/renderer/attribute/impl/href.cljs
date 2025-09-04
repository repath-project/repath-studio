(ns renderer.attribute.impl.href
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/href"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.effects :as-alias effects]
   [renderer.element.db :as element.db]
   [renderer.element.events :as-alias element.events]
   [renderer.events :as events]
   [renderer.notification.events :as-alias notification.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]))

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
      {:title (t [::select-file "Select file"])
       :disabled disabled
       :on-click #(rf/dispatch
                   [::events/file-open
                    {:options {:startIn "pictures"
                               :types [{:accept element.db/image-mime-types}]}
                     :on-success [::success]}])}
      [views/icon "folder"]]]))

(rf/reg-event-fx
 ::success
 (fn [{:keys [db]} [_ _file-handle file]]
   {:db (tool.handlers/activate db :transform)
    ::effects/file-read-as
    [file :data-url {"load" {:on-fire [::element.events/set-attr :href]}
                     "error" {:on-fire [::notification.events/show-exception]}}]}))
