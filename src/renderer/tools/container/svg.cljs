(ns renderer.tools.container.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.h]
   [renderer.tools.base :as tools]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :svg ::tools/container)

(defmethod tools/properties :svg
  []
  {:icon "svg"
   :description "The svg element is a container that defines a new coordinate 
                 system and viewport. It is used as the outermost element of 
                 SVG documents, but it can also be used to embed an SVG fragment 
                 inside an SVG or HTML document."
   :attrs [:overflow]})

(defmethod tools/drag :svg
  [{:keys [adjusted-pointer-pos adjusted-pointer-offset] :as db} e]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        lock-ratio? (contains? (:modifiers e) :ctrl)
        width (abs (- pos-x offset-x))
        height (abs (- pos-y offset-y))
        attrs {:x (min pos-x offset-x)
               :y (min pos-y offset-y)
               :width (if lock-ratio? (min width height) width)
               :height (if lock-ratio? (min width height) height)}]
    (element.h/set-temp db {:tag :svg
                            :type :element
                            :attrs attrs})))

(defmethod tools/render :svg
  [{:keys [attrs children tag] :as el}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        rect-attrs (select-keys attrs [:x :y :width :height])
        text-attrs (select-keys attrs [:x :y])
        filter @(rf/subscribe [:document/filter])
        zoom @(rf/subscribe [:document/zoom])
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

        (not= filter "No a11y filter")
        (assoc :filter (str "url(#" (name filter) ")")))
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
      (for [element (merge child-elements)]
        [tools/render element])]]))

(defmethod tools/bounds :svg
  [{{:keys [x y width height]} :attrs}]
  (let [[x y width height] (mapv units/unit->px [x y width height])]
    [x y (+ x width) (+ y height)]))

(defmethod tools/render-to-string :svg
  [{:keys [attrs children]}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        attrs (->> (dissoc attrs :fill)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (->> (doall (map tools/render-to-string (merge child-elements)))
         (into [:svg attrs]))))
