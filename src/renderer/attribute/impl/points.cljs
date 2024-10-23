(ns renderer.attribute.impl.points
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/points"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.element.events :as-alias element.e]
   [renderer.tool.subs :as-alias tool.s]
   [renderer.ui :as ui]
   [renderer.utils.attribute :as utils.attr]
   [renderer.utils.vec :as vec]))

(defmethod hierarchy/description [:default :points]
  []
  "The points attribute defines a list of points. Each point is defined by a
   pair of number representing a X and a Y coordinate in the user coordinate
   system. If the attribute contains an odd number of coordinates, the last one
   will be ignored.")

(defn remove-nth
  [points i]
  (let [points (str/join " " (flatten (vec/remove-nth points i)))]
    (rf/dispatch [::element.e/set-attr :points points])))

(defn point-row
  [i [x y] points]
  [:div.grid.grid-flow-col.gap-px
   {:style {:grid-template-columns "minmax(0, 40px) 3fr 3fr 26px"}}
   [:label.px-1.bg-transparent i]
   [:input.bg-transparent
    {:key (str "x-" i) :default-value x}]
   [:input.bg-transparent
    {:key (str "y-" i) :default-value y}]
   [:button.button.bg-transparent.text-muted.h-full.rounded
    {:on-click #(remove-nth points i)}
    [ui/icon "times"]]])

(defn points-popover
  [points]
  [:> Popover/Root {:modal true}
   [:> Popover/Trigger
    {:class "form-control-button"}
    [ui/icon "pencil"]]
   [:> Popover/Portal
    [:> Popover/Content
     {:sideOffset 5
      :className "popover-content"
      :align "end"}
     [:div.flex.overflow-hidden
      {:style {:max-height "50vh"}}
      [ui/scroll-area
       [:div.p-4.flex.flex-col.gap-px
        (map-indexed (fn [index point]
                       ^{:key (str "point-" index)}
                       [point-row index point points]) points)]]]
     [:> Popover/Arrow {:class "popover-arrow"}]]]])

(defmethod hierarchy/form-element [:default :points]
  [_ k v {:keys [disabled]}]
  (let [state-idle (= @(rf/subscribe [::tool.s/state]) :idle)]
    [:div.flex.gap-px.w-full
     [v/form-input k (if state-idle v "waiting") {:disabled (or disabled (not v) (not state-idle))}]
     (when v [points-popover (utils.attr/points->vec v)])]))
