(ns renderer.element.impl.container.canvas
  "The main SVG element that hosts all pages."
  (:require
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.event.keyboard :as event.keyboard]
   [renderer.event.pointer :as event.pointer]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.menubar.filters :as filters]
   [renderer.ruler.views :as ruler.views]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.snap.views :as snap.views]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.svg :as utils.svg]))

(derive :canvas ::element.hierarchy/element)

(defmethod element.hierarchy/properties :canvas
  []
  {:description "The canvas is the main SVG container that hosts all elements."
   :attrs [:fill]})

(defn drop-handler!
  "Gathers drop event props.
   https://developer.mozilla.org/en-US/docs/Web/API/DragEvent"
  [^js/DragEvent e]
  (.stopPropagation e)
  (.preventDefault e)

  (rf/dispatch-sync [::tool.events/drag-event {:type (.-type e)
                                               :pointer-pos [(.-pageX e) (.-pageY e)]
                                               :data-transfer (.-dataTransfer e)}]))

(defmethod element.hierarchy/render :canvas
  [el]
  (let [child-elements @(rf/subscribe [::element.subs/filter-visible (:children el)])
        viewbox-attr @(rf/subscribe [::frame.subs/viewbox-attr])
        {:keys [width height]} @(rf/subscribe [::app.subs/dom-rect])
        temp-element @(rf/subscribe [::document.subs/temp-element])
        read-only? @(rf/subscribe [::document.subs/read-only?])
        cursor @(rf/subscribe [::tool.subs/cursor])
        active-tool @(rf/subscribe [::tool.subs/active])
        primary-tool @(rf/subscribe [::tool.subs/primary])
        rotate @(rf/subscribe [::document.subs/rotate])
        grid @(rf/subscribe [::app.subs/grid])
        pointer-handler #(event.pointer/handler! % el)
        snap? @(rf/subscribe [::snap.subs/active?])
        nearest-neighbor @(rf/subscribe [::snap.subs/nearest-neighbor])
        snapped-el-id (-> nearest-neighbor meta :id)
        snapped-el (when snapped-el-id @(rf/subscribe [::element.subs/entity snapped-el-id]))
        key-handler #(rf/dispatch-sync [::tool.events/keyboard-event (event.keyboard/->map %)])]
    [:svg#canvas {:on-pointer-up pointer-handler
                  :on-pointer-down pointer-handler
                  :on-pointer-move pointer-handler
                  :on-key-up key-handler
                  :on-key-down key-handler
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
       ^{:key (:id el)} [element.hierarchy/render el])

     [:defs
      (map (fn [{:keys [id tag attrs]}] [:filter {:id id :key id} [tag attrs]])
           filters/accessibility)]

     (when grid [ruler.views/grid])

     (when-not read-only?
       [:<>
        [tool.hierarchy/render (or primary-tool active-tool)]
        [element.hierarchy/render temp-element]])

     (when snap?
       [:<>
        (when snapped-el
          [utils.svg/bounding-box (:bbox snapped-el) true])
        (when nearest-neighbor
          [snap.views/canvas-label nearest-neighbor])])]))

(defmethod element.hierarchy/render-to-string :canvas
  [el]
  (let [child-elements @(rf/subscribe [::element.subs/filter-visible (:children el)])
        attrs (->> (dissoc (:attrs el) :fill)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (->> (doall (map element.hierarchy/render-to-string child-elements))
         (into [:svg attrs]))))
