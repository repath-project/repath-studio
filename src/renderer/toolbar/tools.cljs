(ns renderer.toolbar.tools
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.components :as comp]
   [renderer.tools.base :as tools]))

(defn button
  [type]
  (let [tool @(rf/subscribe [:tool])
        primary-tool @(rf/subscribe [:primary-tool])
        selected? (= tool type)
        primary? (= primary-tool type)]
    (when (:icon (tools/properties type))
      [:> Tooltip/Root
       [:> Tooltip/Trigger {:asChild true}
        [:span
         [comp/radio-icon-button
          {:active? selected?
           :class (when primary? "border border-accent")
           :icon (:icon (tools/properties type))
           :action #(rf/dispatch [:set-tool type])}]]]
       [:> Tooltip/Portal
        [:> Tooltip/Content
         {:class "tooltip-content"
          :side "top"}
         [:div.flex.gap-2.items-center
          (str/capitalize (name type))
          (when-let [shortcuts (comp/shortcuts [:set-tool type])]
            [:div.text-muted.px-1.py-0.5.text-xs.overlay.rounded-sm
             shortcuts])]
         [:> Tooltip/Arrow {:class "tooltip-arrow"}]]]])))

(defn group
  [group]
  (into [:div.flex]
        (map button (descendants group))))

(def groups
  [::tools/transform
   ::tools/container
   ::tools/renderable
   ::tools/custom
   ::tools/draw
   ::tools/misc])

(defn root
  []
  (into [:div.justify-center.flex-wrap.level-1.toolbar]
        (interpose [:span.v-divider]
                   (map group groups))))
