(ns renderer.toolbar.tools
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.components :as comp]
   [renderer.tool.base :as tool]))

(defn button
  [type]
  (let [tool @(rf/subscribe [:tool])
        primary-tool @(rf/subscribe [:primary-tool])
        selected? (= tool type)
        primary? (= primary-tool type)]
    (when (:icon (tool/properties type))
      [:> Tooltip/Root
       [:> Tooltip/Trigger {:asChild true}
        [:span
         [comp/radio-icon-button
          {:active? selected?
           :class (when primary? "outline-shadow")
           :icon (:icon (tool/properties type))
           :action #(rf/dispatch [:set-tool type])}]]]
       [:> Tooltip/Portal
        [:> Tooltip/Content
         {:class "tooltip-content"
          :sideOffset 5
          :side "top"}
         [:div.flex.gap-2.items-center
          (str/capitalize (name type))]]]])))

(defn group
  [group]
  (into [:div.flex.gap-1]
        (map button group)))

(def groups
  [[:select :edit :pan :zoom]
   [:svg]
   [:circle :ellipse :rect :line :polyline :polygon :image :text]
   [:blob]
   [:brush :pen]
   [:dropper :fill :measure]])

(defn root
  []
  (into [:div.justify-center.flex-wrap.bg-primary.toolbar]
        (interpose [:span.v-divider]
                   (map group groups))))
