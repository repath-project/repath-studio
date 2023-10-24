(ns renderer.debug
  (:require
   [re-frame.core :as rf]
   [renderer.utils.units :as units]
   [re-frame.registrar]
   ["react-fps" :refer [FpsView]]))


(defn fps
  []
  [:div.fps-wrapper
   [:> FpsView #js {:width 240 :height 180}]])

(defn info
  []
  [:div.absolute.top-1.left-2.pointer-events-none
   {:style {:color "#555"}}
   [:div [:strong "Content rect "] @(rf/subscribe [:content-rect])]
   [:div [:strong "Viewbox "] (str (mapv units/->fixed @(rf/subscribe [:frame/viewbox])))]
   [:div [:strong "Mouse position "] (str @(rf/subscribe [:mouse-pos]))]
   [:div [:strong "Adjusted mouse position "] (str (mapv units/->fixed  @(rf/subscribe [:adjusted-mouse-pos])))]
   [:div [:strong "Mouse offset "] (str @(rf/subscribe [:mouse-offset]))]
   [:div [:strong "Adjusted mouse offset "] (str (mapv units/->fixed  @(rf/subscribe [:adjusted-mouse-offset])))]
   [:div [:strong "Mouse drag? "] (str @(rf/subscribe [:drag?]))]
   [:div [:strong "Pan "] (str (mapv units/->fixed @(rf/subscribe [:document/pan])))]
   [:div [:strong "Active tool "] @(rf/subscribe [:tool])]
   [:div [:strong "Primary tool "] @(rf/subscribe [:primary-tool])]
   [:div [:strong "State "] @(rf/subscribe [:state])]
   [:div [:strong "Clicked element "] (:key @(rf/subscribe [:clicked-element]))]])