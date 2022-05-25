(ns repath.studio.tools.blob
  (:require [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            [repath.studio.attrs.views :as attrs]
            [repath.studio.mouse :as mouse]
            [repath.studio.units :as units]
            [repath.studio.elements.handlers :as elements]
            ["svgpath" :as svgpath]
            ["blobs/v2" :as blobs]))

(derive :blob ::tools/custom)

(defmethod attrs/form-element :extraPoints
  [key value]
  [attrs/range-input key value {:min 0
                                :max 50
                                :step "1"}])

(defmethod attrs/form-element :randomness
  [key value]
  [attrs/range-input key value {:min 0
                                :max 50
                                :step "1"}])

(defmethod tools/properties :blob [] {:icon "blob"
                                      :attrs [:x
                                              :y
                                              :seed
                                              :extraPoints
                                              :randomness
                                              :size
                                              :fill
                                              :stroke
                                              :stroke-width
                                              :opacity]})

(defmethod tools/drag :blob
  [{:keys [adjusted-mouse-offset active-document adjusted-mouse-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x (min pos-x offset-x)
               :y (min pos-y offset-y)
               :seed 1
               :extraPoints 8
               :randomness 4
               :size (Math/abs (- pos-x offset-x))
               :fill (tools/rgba fill)
               :stroke (tools/rgba  stroke)}]
    (elements/set-temp db {:type :blob :attrs attrs})))

(defn blob-path [attrs]
  (-> (.svgPath blobs (clj->js (reduce (fn [options [k v]] (assoc options k (int v))) {} (select-keys attrs [:seed :extraPoints :randomness :size]))))
      (svgpath)
      (.translate (units/unit->px (:x attrs)) (units/unit->px (:y attrs)))
      (.toString)))

(defmethod tools/render :blob
  [{:keys [attrs] :as element}]
  [:path (merge {:d (blob-path attrs)
                 :on-mouse-up   #(mouse/event-handler % element)
                 :on-mouse-down #(mouse/event-handler % element)
                 :on-mouse-move #(mouse/event-handler % element)}
                (select-keys attrs [:stroke :fill :stroke-width :id :class :opacity]))])

(defmethod tools/bounds :blob
  [{{:keys [x y size]} :attrs}]
  (let [[x y size] (map units/unit->px [x y size])]
    (mapv units/unit->px [x y (+ x size) (+ y size)])))

(defmethod tools/path :blob
  [element]
  (blob-path (:attrs element)))