(ns renderer.tools.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require [clojure.string :as str]
            [renderer.elements.handlers :as elements]
            [renderer.overlay :as overlay]
            [renderer.tools.base :as tools]
            [renderer.attribute.hierarchy :as hierarchy]
            [renderer.utils.units :as units]
            [renderer.history.handlers :as history]
            [renderer.handlers :as handlers]
            [re-frame.core :as rf]
            [clojure.core.matrix :as matrix]))

(derive :line ::tools/shape)

(defmethod tools/properties :line
  [] {:icon "line"
      :description "The <line> element is an SVG basic shape used to create 
                    a line connecting two points."
      :attrs [:stroke-width
              :stroke-linecap
              :stroke-dasharray
              :opacity]})

(defn create-line
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos active-document] :as db}]
  (let [stroke (get-in db [:documents active-document :stroke])
        [offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 pos-x
               :y2 pos-y
               :stroke stroke}]
    (elements/set-temp db {:type :element :tag :line :attrs attrs})))

(defn update-line-end
  [{:keys [adjusted-mouse-pos] :as db}]
  (let [temp (-> (elements/get-temp db)
                 (assoc-in [:attrs :x2] (first adjusted-mouse-pos))
                 (assoc-in [:attrs :y2] (second adjusted-mouse-pos)))]
    (elements/set-temp db temp)))

(defmethod tools/mouse-move :line
  [db]
  (cond-> db
    (elements/get-temp db) (update-line-end)))

(defmethod tools/mouse-up :line
  [db]
  (if (elements/get-temp db)
    (-> db
        (elements/create-from-temp)
        (history/finalize (str "Create line")))
    (-> db
        (handlers/set-state :create)
        (create-line))))

(defmethod tools/mouse-down :line
  [db]
  (if (elements/get-temp db)
    (history/finalize db (str "Create line"))
    db))

(defmethod tools/drag :line
  [db]
  (create-line db))

(defmethod tools/translate :line
  [element [x y]] (-> element
                      (hierarchy/update-attr :x1 + x)
                      (hierarchy/update-attr :y1 + y)
                      (hierarchy/update-attr :x2 + x)
                      (hierarchy/update-attr :y2 + y)))

(defmethod tools/scale :line
  [element [x y] handler]
  (let [{:keys [x1 y1 x2 y2]} (:attrs element)]
    (cond-> element
      (contains? #{:bottom-right :top-right :middle-right} handler)
      (hierarchy/update-attr (if (> x1 x2) :x1 :x2) + x)

      (contains? #{:bottom-left :top-left :middle-left} handler)
      (-> (hierarchy/update-attr (if (< x1 x2) :x1 :x2) + x))

      (contains? #{:bottom-middle :bottom-right :bottom-left} handler)
      (hierarchy/update-attr (if (> y1 y2) :y1 :y2) + y)

      (contains? #{:top-middle :top-left :top-right} handler)
      (-> (hierarchy/update-attr (if (< y1 y2) :y1 :y2) + y)))))

(defmethod tools/bounds :line
  [{{:keys [x1 y1 x2 y2]} :attrs}]
  (let [[x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
    [(min x1 x2) (min y1 y2) (max x1 x2) (max y1 y2)]))

(defmethod tools/path :line
  [{{:keys [x1 y1 x2 y2]} :attrs}]
  (let [[x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
    (str/join " " ["M" x1 y1
                   "L" x2 y2])))

(defmethod tools/render-edit :line
  [{:keys [attrs key]}]
  (let [{:keys [x1 y1 x2 y2]} attrs
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])
        active-page @(rf/subscribe [:elements/active-page])
        page-pos (mapv units/unit->px
                       [(-> active-page :attrs :x)
                        (-> active-page :attrs :y)])
        [x1 y1] (matrix/add page-pos [x1 y1])
        [x2 y2] (matrix/add page-pos [x2 y2])]
    [:g
     {:key :edit-handlers}
     (map (fn [handler] [overlay/square-handler handler])
          [{:x x1 :y y1 :key :starting-point :type :handler :tag :edit :element key}
           {:x x2 :y y2 :key :ending-point :type :handler :tag :edit :element key}])]))

(defmethod tools/edit :line
  [element [x y] handler]
  (case handler
    :starting-point (-> element
                        (hierarchy/update-attr :x1 + x)
                        (hierarchy/update-attr :y1 + y))
    :ending-point (-> element
                      (hierarchy/update-attr :x2 + x)
                      (hierarchy/update-attr :y2 + y))
    element))
