(ns repath.studio.tools.ruler
  (:require [repath.studio.elements.handlers :as elements]
            [clojure.core.matrix :as matrix]
            [repath.studio.tools.base :as tools]))

(derive :ruler ::tools/edit)

(defmethod tools/properties :ruler [] {:icon "ruler-triangle"})

(defmethod tools/drag :ruler
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos] :as db}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        [diff-x diff-y] (matrix/sub adjusted-mouse-offset adjusted-mouse-pos)
        distance (js/Math.sqrt (+ (* diff-x diff-x) (* diff-y diff-y)))
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 pos-x
               :y2 pos-y}]
    (-> db
        (assoc :state :create)
        (elements/set-temp {:type :line :attrs attrs}))))

#_(defmethod tools/render :ruler
  [{:keys [attrs children key type] :as element}]
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
