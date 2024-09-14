(ns renderer.tool.container.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement"
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as element.h]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.pointer :as pointer]))

(derive :svg ::tool.hierarchy/container)

(defmethod tool.hierarchy/properties :svg
  []
  {:icon "svg"
   :description "The svg element is a container that defines a new coordinate
                 system and viewport. It is used as the outermost element of
                 SVG documents, but it can also be used to embed an SVG fragment
                 inside an SVG or HTML document."
   :attrs [:overflow]})

(defmethod tool.hierarchy/help [:svg :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defmethod tool.hierarchy/drag :svg
  [{:keys [adjusted-pointer-pos adjusted-pointer-offset] :as db} e]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        lock-ratio? (pointer/ctrl? e)
        width (abs (- pos-x offset-x))
        height (abs (- pos-y offset-y))
        attrs {:x (min pos-x offset-x)
               :y (min pos-y offset-y)
               :width (if lock-ratio? (min width height) width)
               :height (if lock-ratio? (min width height) height)}]
    (element.h/set-temp db {:tag :svg
                            :type :element
                            :attrs attrs})))

(defmethod tool.hierarchy/render :svg
  [{:keys [attrs children tag] :as el}]
  (let [child-els @(rf/subscribe [::element.s/filter-visible children])
        rect-attrs (select-keys attrs [:x :y :width :height])
        text-attrs (select-keys attrs [:x :y])
        active-filter @(rf/subscribe [::document.s/filter])
        zoom @(rf/subscribe [::document.s/zoom])
        pointer-handler #(pointer/event-handler % el)]
    [:g
     [:text
      (merge
       (update text-attrs :y - (/ 10 zoom))
       {:on-pointer-up pointer-handler
        :on-pointer-down pointer-handler
        :on-pointer-move pointer-handler
        :fill "#888"
        :font-family "monospace"
        :font-size (/ 12 zoom)}) (or (:name el) (name tag))]

     [:rect
      (merge
       rect-attrs
       {:fill "rgba(0, 0, 0, .1)"
        :transform (str "translate(" (/ 2 zoom) " " (/ 2 zoom) ")")
        :style {:filter (str "blur(" (/ 2 zoom) "px)")}})]

     [:svg
      (cond-> attrs
        :always
        (dissoc :style)

        active-filter
        (assoc :filter (str "url(#" (name active-filter) ")")))
      [:rect
       (merge
        rect-attrs
        {:x 0
         :y 0
         :fill "white"
         :on-pointer-up pointer-handler
         :on-pointer-down #(when (= (.-button %) 2)
                             (pointer/event-handler % el))
         :on-double-click pointer-handler})]
      (for [el child-els]
        ^{:key (:id el)} [tool.hierarchy/render el])]]))
