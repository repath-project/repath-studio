(ns repath.studio.tools.page
  (:require [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.mouse :as mouse]))

(derive :page ::tools/element)

(defmethod tools/properties :page [] {:icon "page"
                                      :description "The page is a top level SVG element with some extra custom attributes."
                                      :attrs [:fill
                                              :overflow]})

(defmethod tools/drag :page
  [{:keys [adjusted-mouse-offset] :as db} _ _ {:keys [adjusted-mouse-pos]}]
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
  [{:keys [attrs children name key type] :as element}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])
        rect-attrs     (select-keys attrs [:x :y :width :height :fill])
        text-attrs     (select-keys attrs [:x :y])
        zoom           @(rf/subscribe [:zoom])]
    [:g {:key key}
     [:rect (merge rect-attrs {:fill "rgba(0, 0, 0, .2)"
                               :transform "translate(1 1)"
                               :style {:filter "blur(1px)"}})];}})]
     [:rect (merge rect-attrs {:on-mouse-up   #(mouse/event-handler % element)
                               :on-mouse-down #(mouse/event-handler % element)})]
     [:text (merge (update text-attrs :y - (/ 10 zoom)) {:on-mouse-up   #(mouse/event-handler % element)
                                                         :on-mouse-down #(mouse/event-handler % element)
                                                         :on-mouse-move #(mouse/event-handler % element)
                                                         :fill "#888"
                                                         :font-size (/ 12 zoom)
                                                         :font-family "Source Code Pro"}) (or name type)]
     [:svg (dissoc attrs :fill)
      (map (fn [element] ^{:key (:key element)} [tools/render element]) (merge child-elements))]]))
