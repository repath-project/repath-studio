(ns renderer.attribute.impl.d
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/points"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["svgpath" :as svgpath]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.events :as-alias events]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [:default :d]
  []
  "The d attribute defines a path to be drawn.")

(def path-commands
  {:m {:label "Move To"
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataMovetoCommands"}
   :l {:label "Line To"
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataLinetoCommands"}
   :v {:label "Vertical Line"
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataLinetoCommands"}
   :h {:label "Horizontal Line"
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataLinetoCommands"}
   :c {:label "Cubic Bézier Curve"
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataCubicBezierCommands"}
   :s {:label "Shortcut Cubic Bézier Curve"
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataCubicBezierCommands"}
   :q {:label "Quadratic Bézier Curve"
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataQuadraticBezierCommands"}
   :t {:label "Shortcut Quadratic Bézier Curve"
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataQuadraticBezierCommands"}
   :a {:label "Elliptical Arc Curve"
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataEllipticalArcCommands"}
   :z {:label "Close Path"
       :url "https://svgwg.org/svg2-draft/paths.html#PathDataClosePathCommand"}})

(defn ->command
  [c]
  (get path-commands (keyword (string/lower-case c))))

(defn remove-segment-by-index
  [path i]
  (set! (.-segments path) (.splice (.-segments path) i 1))
  (rf/dispatch [::element.events/set-attr :p (.toString path)]))

(defmulti segment-form (fn [segment _] (keyword (string/lower-case (first segment)))))

(defmethod segment-form :default
  [segment i]
  [:div.grid.grid-cols-4.gap-px
   [:label.form-element.px-1 "x"]
   [:input.form-element
    {:key (str "x-" i) :default-value (nth segment 1)}]
   [:label.form-element.px-1 "y"]
   [:input.form-element
    {:key (str "y-" i) :default-value (nth segment 2)}]])

(defmethod segment-form :h
  [segment i]
  [:input.form-element {:key (str "width-" i) :default-value (nth segment 1)}])

(defmethod segment-form :v
  [segment i]
  [:input.form-element {:key (str "height-" i) :default-value (nth segment 1)}])

(defmethod segment-form :z [_segment _i])

(defmethod segment-form :a
  [segment i]
  [:div
   [:div.grid.grid-cols-4.gap-px
    [:label.form-element.px-1 "rx"]
    [:input.form-element
     {:key (str "rx-" i) :default-value (nth segment 1)}]
    [:label.form-element.px-1 "ry"]
    [:input.form-element
     {:key (str "ry-" i) :default-value (nth segment 2)}]]
   [:div.grid.grid-cols-2.gap-px
    [:label.form-element.px-1.text-nowrap "x-axis-rotation"]
    [:input.form-element
     {:key (str "x-axis-rotation-" i) :default-value (nth segment 3)}]]
   [:div.grid.grid-cols-2.gap-px
    [:label.form-element.px-1.text-nowrap "large-arc-flag"]
    [:input.form-element
     {:key (str "large-arc-flag-" i) :default-value (nth segment 4)}]]
   [:div.grid.grid-cols-2.gap-px
    [:label.form-element.px-1.text-nowrap "sweep-flag"]
    [:input.form-element
     {:key (str "sweep-flag" i) :default-value (nth segment 5)}]]
   [:div.grid.grid-cols-4.gap-px
    [:label.form-element.px-1 "x"]
    [:input.form-element
     {:key (str "x-" i) :default-value (nth segment 6)}]
    [:label.form-element.px-1. "y"]
    [:input.form-element
     {:key (str "y-" i) :default-value (nth segment 7)}]]])

(defn segment-row
  [i segment path]
  (let [command (first segment)
        {:keys [label url]} (->command command)]
    [:div.my-2
     #_[:div (string/join " " segment)]
     [:div.flex.items-center.justify-between.mb-1
      [:span
       [:span.bg-primary.p-1 (first segment)]
       [:button.p-1.text-inherit
        {:on-click #(rf/dispatch [::events/open-remote-url url])}
        label]
       (if (= command (string/lower-case command))
         "(Relative)" "(Absolute)")]
      [:button.icon-button.small.bg-transparent.text-muted
       {:on-click #(remove-segment-by-index path i)}
       [views/icon "times"]]]
     [segment-form segment i]]))

(defn edit-form
  [v]
  (let [path (-> v svgpath)
        segments (.-segments path)]
    [:div.flex.overflow-hidden
     {:style {:max-height "50vh"}}
     [views/scroll-area
      [:div.p-4.flex.flex-col
       (map-indexed (fn [i segment]
                      ^{:key (str "segment-" i)}
                      [segment-row i segment path]) segments)]]]))

(defmethod attribute.hierarchy/form-element [:default :d]
  [_ k v {:keys [disabled]}]
  (let [idle (= @(rf/subscribe [::tool.subs/state]) :idle)]
    [:div.flex.gap-px.w-full
     [attribute.views/form-input k (if idle v "waiting")
      {:disabled (or disabled (not v) (not idle))}]
     (when v
       [:> Popover/Root {:modal true}
        [:> Popover/Trigger
         {:title "Edit path"
          :class "form-control-button"}
         [views/icon "pencil"]]
        [:> Popover/Portal
         [:> Popover/Content {:sideOffset 5
                              :className "popover-content"
                              :align "end"}
          (when idle [edit-form v])
          [:> Popover/Arrow {:class "popover-arrow"}]]]])]))
