(ns renderer.tool.container.canvas
  "The main SVG element that hosts all pages."
  (:require
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.menubar.filters :as filters]
   [renderer.ruler.views :as ruler.v]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.dom :as dom]
   [renderer.utils.drop :as drop]
   [renderer.utils.keyboard :as keyb]
   [renderer.utils.pointer :as pointer]))

(derive :canvas ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :canvas
  []
  {:description "The canvas is the main SVG container that hosts all elements."
   :attrs [:fill]})

(defmethod tool.hierarchy/render :canvas
  [el]
  (let [_ @(rf/subscribe [::snap.s/in-viewport-tree]) ; TODO: Remove this.
        child-elements @(rf/subscribe [::element.s/filter-visible (:children el)])
        viewbox-attr @(rf/subscribe [::frame.s/viewbox-attr])
        {:keys [width height]} @(rf/subscribe [::app.s/dom-rect])
        hovered-ids @(rf/subscribe [::element.s/hovered])
        selected-elements @(rf/subscribe [::element.s/selected])
        bounds @(rf/subscribe [::element.s/bounds])
        temp-element @(rf/subscribe [::document.s/temp-element])
        elements-area @(rf/subscribe [::element.s/area])
        read-only @(rf/subscribe [::document.s/read-only])
        cursor @(rf/subscribe [::app.s/cursor])
        tool @(rf/subscribe [::app.s/tool])
        primary-tool @(rf/subscribe [::app.s/primary-tool])
        rotate @(rf/subscribe [::document.s/rotate])
        grid @(rf/subscribe [::app.s/grid])
        state @(rf/subscribe [::app.s/state])
        pointer-handler #(pointer/event-handler! % el)
        pivot-point @(rf/subscribe [::app.s/pivot-point])
        snapping-points @(rf/subscribe [::snap.s/points])
        snap-active @(rf/subscribe [::snap.s/active])
        nearest-neighbor @(rf/subscribe [::snap.s/nearest-neighbor])
        debug @(rf/subscribe [::app.s/debug-info])
        select-tool-active (or (= tool :select) (= primary-tool :select))]
    [:svg#canvas {:on-pointer-up pointer-handler
                  :on-pointer-down pointer-handler
                  :on-pointer-move pointer-handler
                  :on-key-up keyb/event-handler!
                  :on-key-down keyb/event-handler!
                  :tab-index 0 ; Enable keyboard events
                  :viewBox viewbox-attr
                  :on-drop drop/event-handler!
                  :on-drag-over dom/prevent-default!
                  :width width
                  :height height
                  :transform (str "rotate(" rotate ")")
                  :cursor cursor
                  :style {:outline 0
                          :background (-> el :attrs :fill)}}
     (for [el child-elements]
       ^{:key (:id el)} [tool.hierarchy/render el])

     [:defs
      (map (fn [{:keys [id tag attrs]}]
             [:filter {:id id :key id} [tag attrs]])
           filters/accessibility)]

     (when-not read-only
       [:<>
        (when (and select-tool-active (contains? #{:default :select :scale} state))
          [:<>
           (for [el selected-elements]
             (when (:bounds el)
               ^{:key (str (:id el) "-bounds")}
               [overlay/bounding-box (:bounds el) false]))

           (for [el hovered-ids]
             (when (:bounds el)
               ^{:key (str (:id el) "-bounds")}
               [overlay/bounding-box (:bounds el) true]))

           (when (and (pos? elements-area) (= state :scale) (seq bounds))
             [overlay/area-label elements-area bounds])

           (when (seq bounds)
             [:<>
              [overlay/wrapping-bounding-box bounds]
              (when (= state :scale) [overlay/size-label bounds])
              [overlay/bounding-handles bounds]])

           (when (and select-tool-active pivot-point)
             [overlay/times pivot-point])])

        (when (or (= tool :edit)
                  (= primary-tool :edit))
          (for [el selected-elements]
            ^{:key (str (:id el) "-edit-points")}
            [:g
             [tool.hierarchy/render-edit el]
             ^{:key (str (:id el) "-centroid")}
             [overlay/centroid el]]))

        [tool.hierarchy/render temp-element]])

     (when debug
       [into [:g]
        (for [snapping-point snapping-points]
          [overlay/point-of-interest snapping-point])])

     (when (and snap-active nearest-neighbor)
       [overlay/times (:point nearest-neighbor)])

     (when grid [ruler.v/grid])]))

(defmethod tool.hierarchy/render-to-string :canvas
  [el]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible (:children el)])
        attrs (->> (dissoc (:attrs el) :fill)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (->> (doall (map tool.hierarchy/render-to-string child-elements))
         (into [:svg attrs]))))
