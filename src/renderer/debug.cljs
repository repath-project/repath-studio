(ns renderer.debug
  (:require
   ["react-fps" :refer [FpsView]]
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.utils.units :as units]))

(defn fps
  []
  [:div.fps-wrapper
   [:> FpsView #js {:width 240 :height 180}]])

(defn info
  []
  (into [:div.absolute.top-1.left-2.pointer-events-none
         {:style {:color "#555"}}]
        (map (fn [[label value]] [:div [:strong.mr-1 label] value])
             [["Content rect"
               @(rf/subscribe [:content-rect])]
              ["Viewbox"
               (str (mapv units/->fixed @(rf/subscribe [:frame/viewbox])))]

              ["Mouse position"
               (str @(rf/subscribe [:mouse-pos]))]

              ["Adjusted mouse position"
               (str (mapv units/->fixed  @(rf/subscribe [:adjusted-mouse-pos])))]

              ["Mouse offset"
               (str @(rf/subscribe [:mouse-offset]))]

              ["Adjusted mouse offset"
               (str (mapv units/->fixed  @(rf/subscribe [:adjusted-mouse-offset])))]

              ["Mouse drag?"
               (str @(rf/subscribe [:drag?]))]

              ["Pan"
               (str (mapv units/->fixed @(rf/subscribe [:document/pan])))]

              ["Active tool"
               @(rf/subscribe [:tool])]

              ["Primary tool"
               @(rf/subscribe [:primary-tool])]

              ["State"
               @(rf/subscribe [:state])]

              ["Clicked element"
               (:key @(rf/subscribe [:clicked-element]))]])))
