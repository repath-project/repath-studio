(ns renderer.tool.container.canvas
  "The main SVG element that hosts all pages."
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.rulers.views :as rulers]
   [renderer.toolbar.filters :as filters]
   [renderer.tool.base :as tool]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.keyboard :as keyb]
   [renderer.utils.pointer :as pointer]))

(derive :canvas ::tool/tool)

(defmethod tool/properties :canvas
  []
  {:description "The canvas is the main SVG container that hosts all elements."
   :attrs [:fill]})

(defmethod tool/render :canvas
  [{:keys [attrs children] :as element}]
  (let [_ @(rf/subscribe [:snap/in-viewport-tree])
        child-elements @(rf/subscribe [:element/filter-visible children])
        viewbox @(rf/subscribe [:frame/viewbox])
        {:keys [width height]} @(rf/subscribe [:dom-rect])
        hovered-elements @(rf/subscribe [:element/hovered])
        selected-elements @(rf/subscribe [:element/selected])
        bounds @(rf/subscribe [:element/bounds])
        temp-element @(rf/subscribe [:document/temp-element])
        elements-area @(rf/subscribe [:element/area])
        read-only? @(rf/subscribe [:document/read-only?])
        cursor @(rf/subscribe [:cursor])
        tool @(rf/subscribe [:tool])
        primary-tool @(rf/subscribe [:primary-tool])
        rotate @(rf/subscribe [:document/rotate])
        grid? @(rf/subscribe [:grid?])
        state @(rf/subscribe [:state])
        pointer-handler #(pointer/event-handler % element)
        pivot-point @(rf/subscribe [:pivot-point])
        snapping-points @(rf/subscribe [:snap/points])
        snap? @(rf/subscribe [:snap/enabled?])
        nearest-neighbor @(rf/subscribe [:snap/nearest-neighbor])
        debug? @(rf/subscribe [:debug-info?])
        select? (or (= tool :select)
                    (= primary-tool :select))]
    [:svg#canvas {:on-pointer-up pointer-handler
                  :on-pointer-down pointer-handler
                  :on-double-click pointer-handler
                  :on-key-up keyb/event-handler
                  :on-key-down keyb/event-handler
                  :tab-index 0 ; Enable keyboard events 
                  :viewBox (str/join " " viewbox)
                  :on-drop pointer-handler
                  :on-drag-over #(.preventDefault %)
                  :width width
                  :height height
                  :transform (str "rotate(" rotate ")")
                  :cursor cursor
                  :style {:outline 0
                          :background (:fill attrs)}}
     (for [el child-elements]
       ^{:key (str (:key el))} [tool/render el])

     [:defs
      (map (fn [{:keys [id tag attrs]}]
             [:filter {:id id :key id} [tag attrs]])
           filters/accessibility)]

     (when-not read-only?
       [:<>
        (when (and select? (contains? #{:default :select :scale} state))
          [:<>
           (for [el selected-elements]
             ^{:key (str (:key el) "-bounds")}
             [overlay/bounding-box (:bounds el) false])

           (for [el hovered-elements]
             ^{:key (str (:key el) "-bounds")}
             [overlay/bounding-box (:bounds el) true])

           (when (and (pos? elements-area) (= state :scale))
             [overlay/area elements-area bounds])

           (when (not-empty (remove zero? bounds))
             [:<>
              [overlay/wrapping-bounding-box bounds]
              (when (= state :scale) [overlay/size bounds])
              [overlay/bounding-handles bounds]])

           (when (and select? pivot-point)
             [overlay/times pivot-point])])

        (when (or (= tool :edit)
                  (= primary-tool :edit))
          (for [el selected-elements]
            ^{:key (str (:key el) "-edit-points")}
            [:g
             [tool/render-edit el]
             ^{:key (str (:key el) "-centroid")}
             [overlay/centroid el]]))

        [tool/render temp-element]])

     (when debug?
       [into [:g]
        (for [snapping-point snapping-points]
          [overlay/point-of-interest snapping-point])])

     (when (and snap? nearest-neighbor)
       [overlay/times (:point nearest-neighbor)])

     (when grid? [rulers/grid])]))
