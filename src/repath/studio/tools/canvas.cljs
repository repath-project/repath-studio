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
  (let [child-elements     @(rf/subscribe [:elements/filter-visible children])
        elements           @(rf/subscribe [:elements])
        viewbox            @(rf/subscribe [:canvas/viewbox])
        content-rect       @(rf/subscribe [:content-rect])
        hovered-elements   @(rf/subscribe [:elements/hovered])
        selected-elements  @(rf/subscribe [:elements/selected])
        bounds             @(rf/subscribe [:elements/bounds])
        temp-element       @(rf/subscribe [:temp-element])
        cursor             @(rf/subscribe [:cursor])
        filter             @(rf/subscribe [:filter])
        zoom               @(rf/subscribe [:zoom])
        rotate             @(rf/subscribe [:rotate])
        debug-info?        @(rf/subscribe [:debug-info?])
        mouse-event        #(.dispatchEvent js/window.parent.document.body.firstChild (new js/MouseEvent (.-type %) %))] 
    [:svg {:on-mouse-up     #(mouse/event-handler % element)
           :on-mouse-down   #(mouse/event-handler % element)
           :on-wheel        #(mouse/event-handler % element)
           :on-click        (fn [e]
                              (mouse-event e)
                              (mouse/event-handler e element))
           ; Enable keyboard events on the svg element 
           :tab-index       0 
           ; We are using the [viewBox](https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/viewBox) attribute to simulate pan and zoom
           :viewBox         (str/join " " viewbox) 
           :width (:width content-rect)
           :height (:height content-rect)
           :transform (str "rotate(" rotate ")")
           :filter (str "url(#" (name filter) ")")
           :style {:user-select "none"
                   :background (:fill attrs)
                   :outline "none"
                   :cursor cursor}}
     (map (fn [element] ^{:key (:key element)} [tools/render element]) child-elements)
     [tools/render temp-element]
     (map (fn [element] ^{:key (str (:key element) "bounds")} [elements/bounding-box (tools/adjusted-bounds elements element) zoom]) hovered-elements)
     (map (fn [element] ^{:key (str (:key element) "selection")} [elements/bounding-box (tools/adjusted-bounds elements element) zoom]) selected-elements)
     (when bounds [elements/edit bounds zoom])
     (when  @(rf/subscribe [:grid?]) [rulers/grid])
     (when debug-info? (into [:g] (map #(elements/point-of-interest % zoom) @(rf/subscribe [:snaping-points]))))
     [:defs (map (fn [{:keys [id type attrs]}] [:filter {:id id :key id} [type attrs]]) filters/accessibility)]]))
