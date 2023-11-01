(ns renderer.attribute.points
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/points"
  (:require
   [renderer.attribute.views :as views]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.components :as comp]
   [renderer.attribute.utils :as utils]
   [clojure.string :as str]
   [renderer.utils.vec :as vec]
   [re-frame.core :as rf]
   ["@radix-ui/react-popover" :as Popover]))

(defmethod hierarchy/description :points
  []
  "The points attribute defines a list of points. Each point is defined by a 
   pair of number representing a X and a Y coordinate in the user coordinate 
   system. If the attribute contains an odd number of coordinates, the last one 
   will be ignored.")

(defn remove-point-by-index
  [points index]
  (let [points (str/join " " (flatten (vec/remove-by-index points index)))]
    (rf/dispatch [:elements/set-attribute :points points])))

(defmethod hierarchy/form-element :points
  [key value disabled?]
  (let [state-default? (= @(rf/subscribe [:state]) :default)]
    [:<>
     [views/form-input {:key key
                        :value (if state-default? value "waiting")
                        :disabled? (or disabled?
                                       (not value)
                                       (not state-default?))}]
     (when value
       [:> Popover/Root {:modal true}
        [:> Popover/Trigger {:asChild true}
         [:button.ml-px.inline-block.level-2.text-muted
          {:style {:flex "0 0 26px"
                   :height "26px"}}
          [comp/icon "pencil" {:class "small"}]]]
        [:> Popover/Portal
         [:> Popover/Content {:sideOffset 5
                              :className "popover-content"
                              :align "end"}
          (when state-default?
            (let [points (utils/points-to-vec value)]
              [:div.flex.flex-col.v-scroll.py-4.pr-2
               {:style {:max-height "500px"}}
               (map-indexed (fn [index [x y]]
                              ^{:key (str "point-" index)}
                              [:div.grid.grid-flow-col.gap-px.mt-px
                               {:style {:grid-template-columns "minmax(0, 40px) 3fr 3fr 26px"}}
                               [:label.px-1.bg-transparent index]
                               [:input.bg-transparent
                                {:key (str "x-" index value) :default-value x}]
                               [:input.bg-transparent
                                {:key (str "y-" index value) :default-value y}]
                               [:button.button.bg-transparent.text-muted
                                {:style {:height "26px"}
                                 :on-click #(remove-point-by-index points index)}
                                [comp/icon "times" {:class "small"}]]]) points)]))
          [:> Popover/Arrow {:class "popover-arrow"}]]]])]))


