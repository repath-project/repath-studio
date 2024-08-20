(ns renderer.attribute.d
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/points"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["svgpath" :as svgpath]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.element.events :as-alias element.e]
   [renderer.ui :as ui]
   [renderer.window.events :as-alias window.e]))

(defmethod hierarchy/description [:default :d]
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
  (get path-commands (keyword (str/lower-case c))))

(defn remove-segment-by-index
  [path i]
  (set! (.-segments path) (.splice (.-segments path) i 1))
  (rf/dispatch [::element.e/set-attr :p (.toString path)]))

(defmulti segment-form (fn [segment _] (keyword (str/lower-case (first segment)))))

(defmethod segment-form :default
  [segment i]
  [:div.grid.grid-cols-4.gap-px
   [:label.px-1 "x"]
   [:input
    {:key (str "x-" i) :default-value (nth segment 1)}]
   [:label.px-1 "y"]
   [:input
    {:key (str "y-" i) :default-value (nth segment 2)}]])

(defmethod segment-form :h
  [segment i]
  [:input {:key (str "width-" i) :default-value (nth segment 1)}])

(defmethod segment-form :v
  [segment i]
  [:input {:key (str "height-" i) :default-value (nth segment 1)}])

(defmethod segment-form :z [_segment _i])


(defmethod segment-form :a
  [segment i]
  [:div
   [:div.grid.grid-cols-4.gap-px
    [:label.px-1 "rx"]
    [:input
     {:key (str "rx-" i) :default-value (nth segment 1)}]
    [:label.px-1 "ry"]
    [:input
     {:key (str "ry-" i) :default-value (nth segment 2)}]]
   [:div.grid.grid-cols-2.gap-px
    [:label.px-1.text-nowrap "x-axis-rotation"]
    [:input
     {:key (str "x-axis-rotation-" i) :default-value (nth segment 3)}]]
   [:div.grid.grid-cols-2.gap-px
    [:label.px-1.text-nowrap "large-arc-flag"]
    [:input
     {:key (str "large-arc-flag-" i) :default-value (nth segment 4)}]]
   [:div.grid.grid-cols-2.gap-px
    [:label.px-1.text-nowrap "sweep-flag"]
    [:input
     {:key (str "sweep-flag" i) :default-value (nth segment 5)}]]
   [:div.grid.grid-cols-4.gap-px
    [:label.px-1 "x"]
    [:input
     {:key (str "x-" i) :default-value (nth segment 6)}]
    [:label.px-1. "y"]
    [:input
     {:key (str "y-" i) :default-value (nth segment 7)}]]])

(defn edit-form
  [v]
  (let [path (-> v svgpath)
        segments (.-segments path)]
    [:div.flex.overflow-hidden
     {:style {:max-height "50vh"}}
     [ui/scroll-area
      [:div.p-4.flex.flex-col
       (map-indexed (fn [i segment]
                      (let [command (first segment)
                            {:keys [label url]} (->command command)]
                        ^{:key (str "segment-" i)}
                        [:div.my-2
                         #_[:div (str/join " " segment)]
                         [:div.flex.items-center.justify-between.mb-1
                          [:span
                           [:span.bg-primary.p-1 (first segment)]
                           [:button.p-1.text-inherit
                            {:on-click #(rf/dispatch [::window.e/open-remote-url url])}
                            label]
                           (if (= command (str/lower-case command))
                             "(Relative)" "(Absolute)")]
                          [:button.icon-button.small.bg-transparent.text-muted
                           {:on-click #(remove-segment-by-index path i)}
                           [ui/icon "times"]]]
                         [segment-form segment i]])) segments)]]]))

(defmethod hierarchy/form-element [:default :d]
  [_ k v disabled?]
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
         [:button.ml-px.inline-block.bg-primary.text-muted.h-full
          {:style {:flex "0 0 26px"}}
          [ui/icon "pencil" {:class "icon small"}]]]
        [:> Popover/Portal
         [:> Popover/Content {:sideOffset 5
                              :className "popover-content"
                              :align "end"}
          (when state-default? [edit-form v])
          [:> Popover/Arrow {:class "popover-arrow"}]]]])]))
