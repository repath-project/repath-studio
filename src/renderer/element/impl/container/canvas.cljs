(ns renderer.element.impl.container.canvas
  "The main SVG element that hosts all pages."
  (:require
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.handle.views :as handle.v]
   [renderer.menubar.filters :as filters]
   [renderer.ruler.views :as ruler.v]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.tool.subs :as-alias tool.s]
   [renderer.utils.dom :as dom]
   [renderer.utils.drop :as drop]
   [renderer.utils.keyboard :as keyb]
   [renderer.utils.overlay :as overlay]
   [renderer.utils.pointer :as pointer]))

(derive :canvas ::hierarchy/element)

(defmethod hierarchy/properties :canvas
  []
  {:description "The canvas is the main SVG container that hosts all elements."
   :attrs [:fill]})

(defmethod hierarchy/render :canvas
  [el]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible (:children el)])
        viewbox-attr @(rf/subscribe [::frame.s/viewbox-attr])
        {:keys [width height]} @(rf/subscribe [::app.s/dom-rect])
        hovered-ids @(rf/subscribe [::element.s/hovered])
        selected-elements @(rf/subscribe [::element.s/selected])
        bounds @(rf/subscribe [::element.s/bounds])
        temp-element @(rf/subscribe [::document.s/temp-element])
        elements-area @(rf/subscribe [::element.s/area])
        read-only @(rf/subscribe [::document.s/read-only])
        cursor @(rf/subscribe [::tool.s/cursor])
        tool @(rf/subscribe [::tool.s/active])
        primary-tool @(rf/subscribe [::tool.s/primary])
        rotate @(rf/subscribe [::document.s/rotate])
        grid @(rf/subscribe [::app.s/grid])
        state @(rf/subscribe [::tool.s/state])
        pointer-handler #(pointer/event-handler! % el)
        pivot-point @(rf/subscribe [::tool.s/pivot-point])
        snap-active @(rf/subscribe [::snap.s/active])
        nearest-neighbor @(rf/subscribe [::snap.s/nearest-neighbor])
        transform-active (or (= tool :transform) (= primary-tool :transform))]
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
       ^{:key (:id el)} [hierarchy/render el])

     [:defs
      (map (fn [{:keys [id tag attrs]}]
             [:filter {:id id :key id} [tag attrs]])
           filters/accessibility)]

     (when-not read-only
       [:<>
        (when (and transform-active (contains? #{:idle :select :scale} state))
          [:<>
           (when (not= state :scale)
             (for [el selected-elements]
               (when (:bounds el)
                 ^{:key (str (:id el) "-bounds")}
                 [overlay/bounding-box (:bounds el) false])))

           (for [el hovered-ids]
             (when (:bounds el)
               ^{:key (str (:id el) "-bounds")}
               [overlay/bounding-box (:bounds el) true]))

           (when (and (pos? elements-area) (= state :scale) (seq bounds))
             [overlay/area-label elements-area bounds])

           (when (seq bounds)
             [:<>
              [handle.v/wrapping-bounding-box bounds]
              (if (= state :scale)
                [overlay/size-label bounds]
                [handle.v/bounding-corners bounds])])

           (when (and transform-active pivot-point)
             [overlay/times pivot-point])])

        (when (or (= tool :edit)
                  (= primary-tool :edit))
          (for [el selected-elements]
            ^{:key (str (:id el) "-edit-points")}
            [:g
             [hierarchy/render-edit el]
             ^{:key (str (:id el) "-centroid")}
             [overlay/centroid el]]))

        [hierarchy/render temp-element]])

     (when (and snap-active nearest-neighbor)
       [overlay/times (:point nearest-neighbor)])

     (when grid [ruler.v/grid])]))

(defmethod hierarchy/render-to-string :canvas
  [el]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible (:children el)])
        attrs (->> (dissoc (:attrs el) :fill)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (->> (doall (map hierarchy/render-to-string child-elements))
         (into [:svg attrs]))))
