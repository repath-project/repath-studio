(ns renderer.attribute.impl.points
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/points"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.attribute :as utils.utils.attribute]
   [renderer.utils.vec :as utils.vec]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [:default :points]
  []
  "The points attribute defines a list of points. Each point is defined by a
   pair of number representing a X and a Y coordinate in the user coordinate
   system. If the attribute contains an odd number of coordinates, the last one
   will be ignored.")

(defn remove-nth
  [points i]
  (let [points (string/join " " (flatten (utils.vec/remove-nth points i)))]
    (rf/dispatch [::element.events/set-attr :points points])))

(defn point-row
  [i [x y] points]
  [:div.grid.grid-flow-col.gap-px
   {:style {:grid-template-columns "minmax(0, 40px) 3fr 3fr 26px"}}
   [:label.form-element.px-1.bg-transparent i]
   [:input.form-element.bg-transparent
    {:key (str "x-" i) :default-value x}]
   [:input.form-element.bg-transparent
    {:key (str "y-" i) :default-value y}]
   [:button.button.bg-transparent.text-muted.h-full.rounded
    {:on-click #(remove-nth points i)}
    [views/icon "times"]]])

(defn points-popover
  [points]
  [:> Popover/Root {:modal true}
   [:> Popover/Trigger
    {:title "Edit points"
     :class "form-control-button"}
    [views/icon "pencil"]]
   [:> Popover/Portal
    [:> Popover/Content
     {:sideOffset 5
      :className "popover-content"
      :align "end"}
     [:div.flex.overflow-hidden
      {:style {:max-height "50vh"}}
      [views/scroll-area
       [:div.p-4.flex.flex-col.gap-px
        (map-indexed (fn [index point]
                       ^{:key (str "point-" index)}
                       [point-row index point points]) points)]]]
     [:> Popover/Arrow {:class "popover-arrow"}]]]])

(defmethod attribute.hierarchy/form-element [:default :points]
  [_ k v {:keys [disabled]}]
  (let [state-idle (= @(rf/subscribe [::tool.subs/state]) :idle)]
    [:div.flex.gap-px.w-full
     [attribute.views/form-input k (if state-idle v "waiting")
      {:disabled (or disabled (not v) (not state-idle))}]
     (when v [points-popover (utils.utils.attribute/points->vec v)])]))
