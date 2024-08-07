(ns renderer.debug
  (:require
   ["react-fps" :refer [FpsView]]
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.document.subs :as-alias document.s]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.utils.units :as units]))

(defn fps
  []
  [:div.fps-wrapper
   [:> FpsView #js {:width 240 :height 180}]])

(defn rows
  []
  [["Dom rect" @(rf/subscribe [:dom-rect])]
   ["Viewbox" (str (mapv units/->fixed @(rf/subscribe [::frame.s/viewbox])))]
   ["Pointer position" (str @(rf/subscribe [:pointer-pos]))]
   ["Adjusted pointer position"
    (str (mapv units/->fixed @(rf/subscribe [:adjusted-pointer-pos])))]
   ["Pointer offset" (str @(rf/subscribe [:pointer-offset]))]
   ["Adjusted pointer offset"
    (str (mapv units/->fixed @(rf/subscribe [:adjusted-pointer-offset])))]
   ["Pointer drag?" (str @(rf/subscribe [:drag?]))]
   ["Pan" (str (mapv units/->fixed @(rf/subscribe [::document.s/pan])))]
   ["Active tool" @(rf/subscribe [:tool])]
   ["Primary tool" @(rf/subscribe [:primary-tool])]
   ["State"  @(rf/subscribe [:state])]
   ["Clicked element" (:key @(rf/subscribe [:clicked-element]))]
   ["Ignored elements" @(rf/subscribe [::document.s/ignored-keys])]
   ["Snap" (map (fn [[k v]] (str k " - " v)) @(rf/subscribe [::snap.s/nearest-neighbor]))]])

(defn info
  []
  (into [:div.absolute.top-1.left-2.pointer-events-none
         {:style {:color "#555"}}]
        (for [[label v] (rows)]
          [:div [:strong.mr-1 label] v])))
