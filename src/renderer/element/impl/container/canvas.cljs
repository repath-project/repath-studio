(ns renderer.element.impl.container.canvas
  "The main SVG element that hosts all pages."
  (:require
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.event.impl.drag :as event.impl.drag]
   [renderer.event.impl.keyboard :as event.impl.keyboard]
   [renderer.event.impl.pointer :as event.impl.pointer]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.menubar.filters :as filters]
   [renderer.ruler.views :as ruler.views]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.snap.views :as snap.views]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.svg :as utils.svg]))

(derive :canvas ::element.hierarchy/element)

(defmethod element.hierarchy/properties :canvas
  []
  {:label (t [::label "Canvas"])
   :description (t [::description
                    "The canvas is the main SVG container that hosts all elements."])
   :attrs [:fill]})

(defmethod element.hierarchy/render :canvas
  [el]
  (let [child-elements @(rf/subscribe [::element.subs/filter-visible (:children el)])
        viewbox-attr @(rf/subscribe [::frame.subs/viewbox-attr])
        {:keys [width height]} @(rf/subscribe [::app.subs/dom-rect])
        read-only? @(rf/subscribe [::document.subs/read-only?])
        cursor @(rf/subscribe [::tool.subs/cursor])
        active-tool @(rf/subscribe [::tool.subs/active])
        cached-tool @(rf/subscribe [::tool.subs/cached])
        rotate @(rf/subscribe [::document.subs/rotate])
        grid @(rf/subscribe [::app.subs/grid])
        pointer-handler (partial event.impl.pointer/handler! el)
        snap? @(rf/subscribe [::snap.subs/active?])
        nearest-neighbor @(rf/subscribe [::snap.subs/nearest-neighbor])
        snapped-el-id (-> nearest-neighbor meta :id)
        snapped-el (when snapped-el-id
                     @(rf/subscribe [::element.subs/entity snapped-el-id]))]
    [:svg#canvas {:on-pointer-up pointer-handler
                  :on-pointer-down pointer-handler
                  :on-pointer-move pointer-handler
                  :on-key-up event.impl.keyboard/handler!
                  :on-key-down event.impl.keyboard/handler!
                  :tab-index 0 ; Enable keyboard events
                  :viewBox viewbox-attr
                  :on-drop event.impl.drag/handler!
                  :on-drag-over event.impl.drag/handler!
                  :width width
                  :height height
                  :transform (str "rotate(" rotate ")")
                  :cursor cursor
                  :style {:outline 0
                          :background (-> el :attrs :fill)}}
     (for [el child-elements]
       ^{:key (:id el)}
       [element.hierarchy/render el])

     (into [:defs]
           (map (fn [{:keys [id tag attrs]}]
                  [:filter {:id id :key id}
                   [tag attrs]]))
           (filters/accessibility))

     (when grid
       [ruler.views/grid])

     (when snap?
       [:<>
        (when snapped-el
          [utils.svg/bounding-box (:bbox snapped-el) true])
        (when nearest-neighbor
          [snap.views/canvas-label nearest-neighbor])])

     (when-not read-only?
       [tool.hierarchy/render (or cached-tool active-tool)])]))

(defmethod element.hierarchy/render-to-string :canvas
  [el]
  (let [child-elements @(rf/subscribe [::element.subs/filter-visible (:children el)])
        attrs (into {} (comp (dissoc (:attrs el) :fill)
                             (remove #(empty? (str (second %))))))]
    (into [:svg attrs]
          (map element.hierarchy/render-to-string)
          child-elements)))
