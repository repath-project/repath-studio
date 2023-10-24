(ns renderer.tools.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.tools.base :as tools]
   [re-frame.registrar]
   ["@radix-ui/react-tooltip" :as Tooltip]))

(defn tool-button [type]
  (let [tool @(rf/subscribe [:tool])
        selected? (= tool type)]
    (when (:icon (tools/properties type))
      [:> Tooltip/Root
       [:> Tooltip/Trigger {:asChild true}
        [:span [comp/radio-icon-button {:active? selected?
                                        :icon (:icon (tools/properties type))
                                        :action #(rf/dispatch [:set-tool type])}]]]
       [:> Tooltip/Portal
        [:> Tooltip/Content {:class "tooltip-content" :side "bottom"}
         type
         [:> Tooltip/Arrow {:class "tooltip-arrow"}]]]]
      

      #_(when (descendants type)
          [:button {:key      (keyword (str "dropdown-" type))
                    :title    type
                    :class    ["icon-button" (when-not selected? "text-muted")]
                    :style    {:background (when selected? styles/overlay)
                               :margin-left "0"
                               :width "16px"}
                    :on-click #(rf/dispatch [:set-tool type])}
           [comp/icon "angle-down"]]))))


(defn toolbar-group [group]
  (into [:div.flex]
        (map tool-button (descendants group))))

(def toolbars [::tools/transform
               ::tools/container
               ::tools/renderable
               ::tools/custom
               ::tools/draw
               ::tools/misc])

(defn toolbar []
  (into [:div.flex.justify-center.flex-wrap.level-2.toolbar]
        (interpose [:span.v-divider]
                   (map (fn [group] [toolbar-group group])
                        toolbars))))