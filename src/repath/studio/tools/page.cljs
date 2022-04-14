(ns repath.studio.tools.page
  (:require [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.units :as units]
            [repath.studio.mouse :as mouse]
            [reagent.dom.server :as dom]))

(derive :page ::tools/element)

(defmethod tools/properties :page [] {:icon "page"
                                      :description "The page is a top level SVG element with some extra custom attributes."
                                      :attrs [:fill
                                              :overflow]})

(defmethod tools/drag :page
  [{:keys [adjusted-mouse-pos adjusted-mouse-offset] :as db}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (Math/abs (- pos-x offset-x))
               :height (Math/abs (- pos-y offset-y))
               :fill   "#ffffff"}]
    (elements/set-temp db {:type :page
                           :name "Page"
                           :attrs attrs})))

(defmethod tools/render :page
  [{:keys [attrs children key type] :as element}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])
        rect-attrs     (select-keys attrs [:width :height :fill])
        text-attrs     (select-keys attrs [:x :y])
        filter         @(rf/subscribe [:filter])
        zoom           @(rf/subscribe [:zoom])]
    [:g {:key key}
     [:text (merge (update text-attrs :y - (/ 10 zoom)) {:on-mouse-up     #(mouse/event-handler % element)
                                                         :on-mouse-down   #(mouse/event-handler % element)
                                                         :on-mouse-move   #(mouse/event-handler % element)
                                                         :fill "#888"
                                                         :font-size (/ 12 zoom)
                                                         :font-family "Source Code Pro, monospace"}) (or (:name element) type)]
     [:svg  (cond-> attrs
              :always (dissoc :fill)
              (not= filter :no-filter) (assoc :filter (str "url(#" (name filter) ")")))
      (when (:fill attrs) [:rect (merge rect-attrs {:on-mouse-up   #(mouse/event-handler % element)
                                                    :on-mouse-down #(mouse/event-handler % element)
                                                    :on-double-click #(mouse/event-handler % element)
})])
      (map (fn [element] ^{:key (:key element)} [tools/render element]) (merge child-elements))]]))

(defmethod tools/area :page
  [{{:keys [width height]} :attrs}]
  (apply * (map units/unit->px [width height])))

(defmethod tools/render-to-string :page
  [{:keys [attrs children] :as element}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])]
    (dom/render-to-static-markup [:svg (dissoc attrs :fill) (map (fn [element] ^{:key (:key element)} [tools/render element]) (merge child-elements))])))