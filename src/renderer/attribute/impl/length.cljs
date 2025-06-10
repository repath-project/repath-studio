(ns renderer.attribute.impl.length
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#length"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]
   [renderer.utils.i18n :refer [t]]))

(derive :x ::length)
(derive :y ::length)
(derive :x1 ::length)
(derive :y1 ::length)
(derive :x2 ::length)
(derive :y2 ::length)
(derive :cx ::length)
(derive :cy ::length)
(derive :dx ::length)
(derive :dy ::length)

(derive ::positive-length ::length)

(derive :width ::positive-length)
(derive :height ::positive-length)
(derive :stroke-width ::positive-length)
(derive :r ::positive-length)
(derive :rx ::positive-length)
(derive :ry ::positive-length)

(defmethod attribute.hierarchy/form-element [:default ::length]
  [_ k v {:keys [disabled placeholder]}]
  [:div.flex.w-full.gap-px
   [attribute.views/form-input k v
    {:disabled disabled
     :placeholder (if v placeholder "multiple")}]
   [:div.flex.gap-px
    [:button.form-control-button
     {:disabled disabled
      :title (t [::decrease "Decrease"])
      :on-pointer-down #(rf/dispatch [::element.events/update-attr k dec])}
     [views/icon "minus"]]
    [:button.form-control-button
     {:disabled disabled
      :title (t [::increase "Increase"])
      :on-click #(rf/dispatch [::element.events/update-attr k inc])}
     [views/icon "plus"]]]])

(defmethod attribute.hierarchy/update-attr ::length
  [el k f & more]
  (update-in el [:attrs k] #(apply utils.length/transform % f more)))

(defmethod attribute.hierarchy/update-attr ::positive-length
  [el k f & more]
  (update-in el [:attrs k] utils.length/transform (fn [v] (max 0 (apply f v more)))))
