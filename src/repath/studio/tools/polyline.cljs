(ns repath.studio.tools.polyline
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]
            [clojure.string :as str]))

(derive :polyline ::tools/shape)

(defn points-to-vec
  [points]
  (str/split (str/triml points) #"\s+"))

(defmethod tools/properties :polyline [] {:icon "polyline"
                                          :description "The <polyline> SVG element is an SVG basic shape that creates straight lines connecting several points."
                                          :attrs [:stroke-width
                                                  :fill
                                                  :stroke
                                                  :stroke-linejoin
                                                  :opacity]})

(defmethod tools/click :polyline
  [{:keys [state active-document] :as db} event element {:keys [adjusted-mouse-pos fill stroke]}]
  (js/console.log event)
  (if (get-in db [:documents active-document :temp-element])
    (update-in db [:documents active-document :temp-element :attrs :points] #(str % " " (str/join " " adjusted-mouse-pos)))
    (-> db
        (assoc :state :create)
        (elements/set-temp {:type :polyline :attrs {:points (str/join " " adjusted-mouse-pos)
                                                    :stroke (tools/rgba stroke)
                                                    :fill (tools/rgba stroke)}}))))

(defmethod tools/drag-end :polyline
  [{:keys [state active-document] :as db} event element {:keys [adjusted-mouse-pos adjusted-mouse-offset fill stroke]}]
  (if (get-in db [:documents active-document :temp-element])
    (update-in db [:documents active-document :temp-element :attrs :points] #(str % " " (str/join " " adjusted-mouse-pos)))
    (-> db
        (assoc :state :create)
        (elements/set-temp {:type :polyline :attrs {:points (str/join " " (concat adjusted-mouse-pos adjusted-mouse-offset))
                                                    :stroke (tools/rgba stroke)
                                                    :fill (tools/rgba stroke)}}))))

(defmethod tools/drag :polyline
  [{:keys [adjusted-mouse-offset active-document] :as db} event element {:keys [adjusted-mouse-pos fill stroke] :as tool-data}]
  (let [points (get-in db [:documents active-document :temp-element :attrs :points])
        attrs {:points (str/join " " (concat adjusted-mouse-pos adjusted-mouse-offset))
               :stroke (tools/rgba stroke)
               :fill (tools/rgba stroke)}]
    (if points
      (tools/mouse-move db event element tool-data)
      (-> db
          (assoc :state :create)
          (elements/set-temp {:type :polyline :attrs attrs})))))

(defmethod tools/mouse-move :polyline
  [{active-document :active-document :as db} event element {:keys [mouse-pos adjusted-mouse-pos stroke]}]
  (let [points (get-in db [:documents active-document :temp-element :attrs :points])]
    (if points
      (assoc-in db [:documents active-document :temp-element :attrs :points] (str/join " " (concat (drop-last 2 (points-to-vec points)) adjusted-mouse-pos)))
      db)))

(defmethod tools/move :polyline
  [element [x y]] (-> element
                      (update-in [:attrs :points] (fn [val]
                                                    (->> val
                                                         (points-to-vec)
                                                         (partition 2)
                                                         (reduce (fn [points point] (concat points [(units/transform #(+ x %) (first point)) (units/transform #(+ y %) (second point))])) [])
                                                         (concat)
                                                         (str/join " "))))))

(defmethod tools/bounds :polyline
  [_ element]
  (:bounds element))

(defmethod tools/path :line
  [{{:keys [x1 y1 x2 y2]} :attrs}]
  (str "M" x1 "," y1 " L" x2 "," y2))
