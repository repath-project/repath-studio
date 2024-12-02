(ns renderer.element.impl.container.canvas
  "The main SVG element that hosts all pages."
  (:require
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.element.subs :as-alias s]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.menubar.filters :as filters]
   [renderer.ruler.views :as ruler.v]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.snap.views :as snap.v]
   [renderer.tool.events :as-alias tool.e]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.s]
   [renderer.utils.keyboard :as keyb]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.svg :as svg]))

(derive :canvas ::hierarchy/element)

(defmethod hierarchy/properties :canvas
  []
  {:description "The canvas is the main SVG container that hosts all elements."
   :attrs [:fill]})

(defn drop-handler!
  "Gathers drop event props.
   https://developer.mozilla.org/en-US/docs/Web/API/DragEvent"
  [^js/DragEvent e]
  (.stopPropagation e)
  (.preventDefault e)

  (rf/dispatch-sync [::tool.e/drag-event {:type (.-type e)
                                          :pointer-pos [(.-pageX e) (.-pageY e)]
                                          :data-transfer (.-dataTransfer e)}]))

(defmethod hierarchy/render :canvas
  [el]
  (let [child-elements @(rf/subscribe [::s/filter-visible (:children el)])
        viewbox-attr @(rf/subscribe [::frame.s/viewbox-attr])
        {:keys [width height]} @(rf/subscribe [::app.s/dom-rect])
        temp-element @(rf/subscribe [::document.s/temp-element])
        read-only? @(rf/subscribe [::document.s/read-only?])
        cursor @(rf/subscribe [::tool.s/cursor])
        active-tool @(rf/subscribe [::tool.s/active])
        primary-tool @(rf/subscribe [::tool.s/primary])
        rotate @(rf/subscribe [::document.s/rotate])
        grid @(rf/subscribe [::app.s/grid])
        pointer-handler #(pointer/event-handler! % el)
        snap? @(rf/subscribe [::snap.s/active?])
        nearest-neighbor @(rf/subscribe [::snap.s/nearest-neighbor])
        snapped-el-id (-> nearest-neighbor meta :id)
        snapped-el (when snapped-el-id @(rf/subscribe [::s/entity snapped-el-id]))]
    [:svg#canvas {:on-pointer-up pointer-handler
                  :on-pointer-down pointer-handler
                  :on-pointer-move pointer-handler
                  :on-key-up keyb/event-handler!
                  :on-key-down keyb/event-handler!
                  :tab-index 0 ; Enable keyboard events
                  :viewBox viewbox-attr
                  :on-drop drop-handler!
                  :on-drag-over #(.preventDefault %)
                  :width width
                  :height height
                  :transform (str "rotate(" rotate ")")
                  :cursor cursor
                  :style {:outline 0
                          :background (-> el :attrs :fill)}}
     (for [el child-elements]
       ^{:key (:id el)} [hierarchy/render el])

     [:defs
      (map (fn [{:keys [id tag attrs]}] [:filter {:id id :key id} [tag attrs]])
           filters/accessibility)]

     (when-not read-only?
       [:<>
        [tool.hierarchy/render (or primary-tool active-tool)]
        [hierarchy/render temp-element]])

     (when snap?
       [:<>
        (when snapped-el
          [svg/bounding-box (:bounds snapped-el) true])
        (when nearest-neighbor
          [snap.v/canvas-label nearest-neighbor])])

     (when grid [ruler.v/grid])]))

(defmethod hierarchy/render-to-string :canvas
  [el]
  (let [child-elements @(rf/subscribe [::s/filter-visible (:children el)])
        attrs (->> (dissoc (:attrs el) :fill)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (->> (doall (map hierarchy/render-to-string child-elements))
         (into [:svg attrs]))))
