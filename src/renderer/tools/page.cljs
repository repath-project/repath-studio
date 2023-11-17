(ns renderer.tools.page
  (:require
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]
   [renderer.element.handlers :as elements]
   [renderer.utils.mouse :as mouse]
   [clojure.core.matrix :as matrix]
   [renderer.utils.units :as units]
   [clojure.string :as str]
   [reagent.dom.server :as dom]
   [goog.string :as gstring]))

(derive :page ::tools/container)

(defmethod tools/properties :page
  []
  {:icon "page"
   :description "The page is a top level SVG element with some extra custom 
                 attributes."
   :attrs [:overflow]})

(defmethod tools/drag :page
  [{:keys [adjusted-mouse-pos adjusted-mouse-offset] :as db}
   event]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        lock-ratio? (contains? (:modifiers event) :ctrl)
        width (abs (- pos-x offset-x))
        height (abs (- pos-y offset-y))
        attrs {:x (min pos-x offset-x)
               :y (min pos-y offset-y)
               :width (if lock-ratio? (min width height) width)
               :height (if lock-ratio? (min width height) height)
               :fill "#ffffff"}]
    (elements/set-temp db {:tag :page
                           :type :element
                           :name "Page"
                           :attrs attrs})))

(defmethod tools/bounds :page
  [{:keys [attrs]}]
  (let [{:keys [x y width height stroke-width stroke]} attrs
        [x y width height stroke-width-px] (mapv units/unit->px
                                                 [x y width height stroke-width])
        stroke-width-px (if (str/blank? stroke-width) 1 stroke-width-px)
        [x y] (matrix/sub [x y] (/ (if (str/blank? stroke) 0 stroke-width-px) 2))
        [width height] (matrix/add [width height]
                                   (if (str/blank? stroke) 0 stroke-width-px))]
    (mapv units/unit->px [x y (+ x width) (+ y height)])))

(defmethod tools/render :page
  [{:keys [attrs children type] :as element}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        rect-attrs (select-keys attrs [:x :y :width :height])
        text-attrs (select-keys attrs [:x :y])
        filter @(rf/subscribe [:document/filter])
        zoom @(rf/subscribe [:document/zoom])
        mouse-handler #(mouse/event-handler % element)]
    [:g
     [:text
      (merge
       (update text-attrs :y - (/ 10 zoom))
       {:on-pointer-up mouse-handler
        :on-pointer-down mouse-handler
        :on-pointer-move mouse-handler
        :fill "#888"
        :font-family "monospace"
        :font-size (/ 12 zoom)}) (or (:name element) type)]

     [:rect
      (merge
       rect-attrs
       {:fill "rgba(0, 0, 0, .1)"
        :transform (str "translate(" (/ 2 zoom) " " (/ 2 zoom) ")")
        :style {:filter (str "blur(" (/ 2 zoom) "px)")}})]

     [:svg
      (cond-> (dissoc attrs :style)
        :always
        (dissoc :fill)

        (not= filter "No a11y filter")
        (assoc :filter (str "url(#" (name filter) ")")))
      (when (:fill attrs)
        [:rect
         (merge
          rect-attrs
          {:x 0
           :y 0
           :fill (:fill attrs)
           :on-pointer-up mouse-handler
           :on-double-click mouse-handler})])
      (map (fn [element] [tools/render element]) (merge child-elements))]]))

(defmethod tools/area :page [])

(defmethod tools/render-to-string :page
  [{:keys [attrs children]}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        attrs (->> (dissoc attrs :fill)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (gstring/unescapeEntities
     (dom/render-to-static-markup
      [:svg attrs
       (doall (map tools/render-to-string (merge child-elements)))]))))