(ns renderer.tools.shape.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as handlers]
   [renderer.history.handlers :as history]
   [renderer.tools.base :as tools]
   [renderer.tools.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.units :as units]))

(derive :line ::tools/shape)

(defmethod tools/properties :line
  []
  {:icon "line-alt"
   :description "The <line> element is an SVG basic shape used to create a line 
                 connecting two points."
   :attrs [:stroke-width
           :stroke-linecap
           :stroke-dasharray
           :opacity]})

(defn create-line
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos active-document] :as db}]
  (let [stroke (get-in db [:documents active-document :stroke])
        [offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 pos-x
               :y2 pos-y
               :stroke stroke}]
    (element.h/set-temp db {:type :element :tag :line :attrs attrs})))

(defn update-line-end
  [{:keys [adjusted-pointer-pos] :as db}]
  (let [temp (-> (element.h/get-temp db)
                 (assoc-in [:attrs :x2] (first adjusted-pointer-pos))
                 (assoc-in [:attrs :y2] (second adjusted-pointer-pos)))]
    (element.h/set-temp db temp)))

(defmethod tools/pointer-move :line
  [db]
  (cond-> db
    (element.h/get-temp db) (update-line-end)))

(defmethod tools/pointer-up :line
  [db]
  (if (element.h/get-temp db)
    (-> db
        element.h/add
        (history/finalize "Create line"))
    (-> db
        (handlers/set-state :create)
        create-line)))

(defmethod tools/pointer-down :line
  [db]
  (cond-> db
    (element.h/get-temp db)
    (history/finalize "Create line")))

(defmethod tools/drag :line
  [db]
  (create-line db))

(defmethod tools/translate :line
  [el [x y]]
  (-> el
      (hierarchy/update-attr :x1 + x)
      (hierarchy/update-attr :y1 + y)
      (hierarchy/update-attr :x2 + x)
      (hierarchy/update-attr :y2 + y)))

(defmethod tools/scale :line
  [el ratio pivot-point]
  (let [{:keys [x1 y1 x2 y2]} (:attrs el)
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])
        dimentions (bounds/->dimensions (tools/bounds el))
        [x y] (mat/sub dimentions (mat/mul dimentions ratio))
        pivot-diff (mat/sub pivot-point dimentions)
        offset (mat/sub pivot-diff (mat/mul pivot-diff ratio))]
    (-> el
        (hierarchy/update-attr (if (< x1 x2) :x1 :x2) + x)
        (hierarchy/update-attr (if (< y1 y2) :y1 :y2) + y)
        (tools/translate offset))))

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
        active-page @(rf/subscribe [:element/active-page])
        page-pos (mapv units/unit->px
                       [(-> active-page :attrs :x)
                        (-> active-page :attrs :y)])
        [x1 y1] (mat/add page-pos [x1 y1])
        [x2 y2] (mat/add page-pos [x2 y2])]
    [:g
     {:key :edit-handlers}
     (map (fn [handler] [overlay/square-handler handler])
          [{:x x1
            :y y1
            :key :starting-point
            :type :handler
            :tag :edit
            :element key}
           {:x x2
            :y y2
            :key :ending-point
            :type :handler
            :tag :edit
            :element key}])]))

(defmethod tools/edit :line
  [el [x y] handler]
  (case handler
    :starting-point
    (-> el
        (hierarchy/update-attr :x1 + x)
        (hierarchy/update-attr :y1 + y))

    :ending-point
    (-> el
        (hierarchy/update-attr :x2 + x)
        (hierarchy/update-attr :y2 + y))
    el))
