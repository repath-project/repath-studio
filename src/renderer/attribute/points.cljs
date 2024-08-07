(ns renderer.attribute.points
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/points"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.components :as comp]
   [renderer.element.events :as-alias element.e]
   [renderer.utils.attribute :as utils.attr]
   [renderer.utils.vec :as vec]))

(defmethod hierarchy/description :points
  []
  "The points attribute defines a list of points. Each point is defined by a
   pair of number representing a X and a Y coordinate in the user coordinate
   system. If the attribute contains an odd number of coordinates, the last one
   will be ignored.")

(defn remove-nth
  [points i]
  (let [points (str/join " " (flatten (vec/remove-nth points i)))]
    (rf/dispatch [::element.e/set-attr :points points])))

(defmethod hierarchy/form-element :points
  [k v disabled?]
  (let [state-default? (= @(rf/subscribe [:state]) :default)]
    [:<>
     [v/form-input
      {:key k
       :value (if state-default? v "waiting")
       :disabled? (or disabled?
                      (not v)
                      (not state-default?))}]
     (when v
       [:> Popover/Root {:modal true}
        [:> Popover/Trigger {:asChild true}
         [:button.ml-px.inline-block.bg-primary.text-muted
          {:style {:flex "0 0 26px"}}
          [comp/icon "pencil" {:class "icon small"}]]]
        [:> Popover/Portal
         [:> Popover/Content {:sideOffset 5
                              :className "popover-content"
                              :align "end"}
          (when state-default?
            (let [points (utils.attr/points->vec v)]
              [:div.flex.flex-col.py-4.pr-2.overflow-y-auto
               {:style {:max-height "50vh"}}
               (map-indexed (fn [index [x y]]
                              ^{:key (str "point-" index)}
                              [:div.grid.grid-flow-col.gap-px.mt-px
                               {:style {:grid-template-columns "minmax(0, 40px) 3fr 3fr 26px"}}
                               [:label.px-1.bg-transparent index]
                               [:input.bg-transparent
                                {:key (str "x-" index v) :default-value x}]
                               [:input.bg-transparent
                                {:key (str "y-" index v) :default-value y}]
                               [:button.button.bg-transparent.text-muted.h-full
                                {:on-click #(remove-nth points index)}
                                [comp/icon "times"]]]) points)]))
          [:> Popover/Arrow {:class "popover-arrow"}]]]])]))
