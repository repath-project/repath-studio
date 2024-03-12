(ns renderer.attribute.d
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/points"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["svgpath" :as svgpath]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.components :as comp]))

(defmethod hierarchy/description :d
  []
  "The d attribute defines a path to be drawn.")

(def path-commands {:m "Move To"
                    :l "Line To"
                    :v "Vertical Line"
                    :h "Horizontal Line"
                    :c "Cubic Bézier"
                    :s "Several Cubic Bézier"
                    :q "Quadratic Bézier"
                    :t "Several Quadratic Bézier"
                    :a "Arc"
                    :z "Close Path"})

(defn ->command
  [char]
  (get path-commands (keyword (str/lower-case char))))

(defn remove-segment-by-index
  [path i]
  (set! (.-segments path) (.splice (.-segments path) i 1))
  (rf/dispatch [:element/set-attr :p (.toString path)]))

(defmulti segment-form (fn [segment _] (keyword (str/lower-case (first segment)))))

(defmethod segment-form :default
  [segment i]
  [:div.grid.grid-cols-4
   {:style {:grid-template-columns "1fr 2fr 1fr 2fr"}}
   [:label.px-1 "x"]
   [:input
    {:key (str "x-" i) :default-value (nth segment 1)}]
   [:label.px-1 "y"]
   [:input
    {:key (str "y-" i) :default-value (nth segment 2)}]])

(defmethod segment-form :h
  [segment i]
  [:div.mb-px
   [:input
    {:key (str "width-" i) :default-value (nth segment 1)}]])

(defmethod segment-form :v
  [segment i]
  [:div.mb-px
   [:input
    {:key (str "height-" i) :default-value (nth segment 1)}]])

(defmethod segment-form :z
  [_segment _i]
  [:div.grid.grid-cols-2.mb-px])


(defmethod segment-form :a
  [segment i]
  [:div
   [:div.grid.grid-cols-4.mb-px
    {:style {:grid-template-columns "1fr 2fr 1fr 2fr"}}
    [:label.px-1.mr-px "rx"]
    [:input
     {:key (str "rx-" i) :default-value (nth segment 1)}]
    [:label.px-1.mr-px "ry"]
    [:input
     {:key (str "ry-" i) :default-value (nth segment 2)}]]
   [:div.grid.grid-cols-2.mb-px
    {:style {:grid-template-columns "2fr 1fr"}}
    [:label.px-1.text-nowrap.mr-px "x-axis-rotation"]
    [:input
     {:key (str "x-axis-rotation-" i) :default-value (nth segment 3)}]]
   [:div.grid.grid-cols-2.mb-px
    {:style {:grid-template-columns "2fr 1fr"}}
    [:label.px-1.text-nowrap.mr-px "large-arc-flag"]
    [:input
     {:key (str "large-arc-flag-" i) :default-value (nth segment 4)}]]
   [:div.grid.grid-cols-2.mb-px
    {:style {:grid-template-columns "2fr 1fr"}}
    [:label.px-1.text-nowrap.mr-px "sweep-flag"]
    [:input
     {:key (str "sweep-flag" i) :default-value (nth segment 5)}]]
   [:div.grid.grid-cols-4
    {:style {:grid-template-columns "1fr 2fr 1fr 2fr"}}
    [:label.px-1.mr-px "x"]
    [:input
     {:key (str "x-" i) :default-value (nth segment 6)}]
    [:label.px-1.mr-px "y"]
    [:input
     {:key (str "y-" i) :default-value (nth segment 7)}]]])

(defmethod hierarchy/form-element :d
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
          {:style {:flex "0 0 26px"
                   :height "26px"}}
          [comp/icon "pencil" {:class "icon small"}]]]
        [:> Popover/Portal
         [:> Popover/Content {:sideOffset 5
                              :className "popover-content"
                              :align "end"}
          (when state-default?
            (let [path (-> v svgpath)
                  segments (.-segments path)]
              [:div.flex.flex-col.v-scroll.p-3
               {:style {:max-height "500px"}}
               (map-indexed (fn [i segment]
                              ^{:key (str "point-" i)}
                              [:div.grid.grid-flow-col.gap-px.my-1
                               {:style {:grid-template-columns "26px 2fr 26px"}}
                               [:div.text-right.p-1 (first segment)]
                               [segment-form segment i]
                               [:button.button.bg-transparent.text-muted
                                {:style {:height "26px"}
                                 :on-click #(remove-segment-by-index path i)}
                                [comp/icon "times" {:class "icon small"}]]]) segments)]))
          [:> Popover/Arrow {:class "popover-arrow"}]]]])]))
