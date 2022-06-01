(ns repath.studio.tools.canvas
  (:require [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            [repath.studio.rulers.views :as rulers]
            [clojure.string :as str]
            [repath.studio.filters :as filters]
            [repath.studio.elements.views :as elements]
            [repath.studio.mouse :as mouse]))

(derive :canvas ::tools/tool)

(defmethod tools/properties :canvas [] {:description "The canvas is the main SVG element that host all pages."
                                        :attrs [:fill]})

(defmethod tools/render :canvas
  [{:keys [attrs children] :as element}]
  (let [child-elements      @(rf/subscribe [:elements/filter-visible children])
        elements            @(rf/subscribe [:elements])
        viewbox             @(rf/subscribe [:canvas/viewbox])
        content-rect        @(rf/subscribe [:content-rect])
        hovered-or-selected @(rf/subscribe [:elements/hovered-or-selected])
        selected-elements   @(rf/subscribe [:elements/selected])
        bounds              @(rf/subscribe [:elements/bounds])
        area                @(rf/subscribe [:elements/area])
        temp-element        @(rf/subscribe [:temp-element])
        cursor              @(rf/subscribe [:cursor])
        tool                @(rf/subscribe [:tool])
        cached-tool         @(rf/subscribe [:cached-tool])
        rotate              @(rf/subscribe [:rotate])
        debug-info?         @(rf/subscribe [:debug-info?])]
    [:svg {:on-mouse-up #(mouse/event-handler % element)
           :on-mouse-down #(mouse/event-handler % element)
           :on-wheel #(mouse/event-handler % element)
           :tab-index 0 ; Enable keyboard events on the svg element 
           :viewBox (str/join " " viewbox)
           :width (:width content-rect)
           :height (:height content-rect)
           :transform (str "rotate(" rotate ")")
           :style {:background (:fill attrs)
                   :outline "none"
                   :cursor cursor}}
     (map (fn [element] ^{:key (:key element)} [tools/render element]) child-elements)
       [:<>
        [tools/render temp-element]
        (when (not= tool :dropper)
          [:<>
           (when  @(rf/subscribe [:grid?]) [rulers/grid])
           (map (fn [element] ^{:key (str (:key element) "bounds")} [elements/bounding-box (tools/adjusted-bounds element elements)]) hovered-or-selected)
           (when (and bounds (or (= tool :select) (= cached-tool :select)))
             [:<>
              (when (> area 0) [elements/area area bounds])
              (when (not-empty (filter (comp not zero?) bounds))
                [:<>
                  [elements/size bounds]
                  [elements/bounding-handlers bounds]])])
           (when (or (= tool :edit) (= cached-tool :edit)) [tools/render-edit (first selected-elements)])
           (when debug-info? (into [:g] (map #(elements/point-of-interest %) @(rf/subscribe [:snaping-points]))))])]
     [:defs (map (fn [{:keys [id tag attrs]}] [:filter {:id id :key id} [tag attrs]]) filters/accessibility)]]))
