(ns renderer.tool.shape.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.app.handlers :as app.h]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.units :as units]))

(derive :line ::tool.hierarchy/shape)

(defmethod tool.hierarchy/properties :line
  []
  {:icon "line-alt"
   :description "The <line> element is an SVG basic shape used to create a line
                 connecting two points."
   :attrs [:stroke
           :stroke-width
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
    (element.h/assoc-temp db {:type :element
                              :tag :line
                              :attrs attrs})))

(defn update-line-end
  [{:keys [adjusted-pointer-pos] :as db}]
  (let [temp (-> (element.h/get-temp db)
                 (assoc-in [:attrs :x2] (first adjusted-pointer-pos))
                 (assoc-in [:attrs :y2] (second adjusted-pointer-pos)))]
    (element.h/assoc-temp db temp)))

(defmethod tool.hierarchy/pointer-move :line
  [db]
  (cond-> db
    (element.h/get-temp db) (update-line-end)))

(defmethod tool.hierarchy/pointer-up :line
  [db _e]
  (cond
    (element.h/get-temp db)
    (-> db
        (element.h/add)
        (app.h/set-tool :select)
        (app.h/set-state :default)
        (app.h/explain "Create line"))

    (:pointer-offset db)
    (-> db
        (app.h/set-state :create)
        (create-line))

    :else db))

(defmethod tool.hierarchy/pointer-down :line
  [db _e]
  (cond-> db
    (element.h/get-temp db)
    (app.h/explain "Create line")))

(defmethod tool.hierarchy/drag :line
  [db]
  (create-line db))

(defmethod tool.hierarchy/translate :line
  [el [x y]]
  (-> el
      (hierarchy/update-attr :x1 + x)
      (hierarchy/update-attr :y1 + y)
      (hierarchy/update-attr :x2 + x)
      (hierarchy/update-attr :y2 + y)))

(defmethod tool.hierarchy/scale :line
  [el ratio pivot-point]
  (let [{:keys [x1 y1 x2 y2]} (:attrs el)
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])
        dimentions (bounds/->dimensions (tool.hierarchy/bounds el))
        [x y] (mat/sub dimentions (mat/mul dimentions ratio))
        pivot-diff (mat/sub pivot-point dimentions)
        offset (mat/sub pivot-diff (mat/mul pivot-diff ratio))]
    (-> el
        (hierarchy/update-attr (if (< x1 x2) :x1 :x2) + x)
        (hierarchy/update-attr (if (< y1 y2) :y1 :y2) + y)
        (tool.hierarchy/translate offset))))

(defmethod tool.hierarchy/path :line
  [{{:keys [x1 y1 x2 y2]} :attrs}]
  (let [[x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
    (str/join " " ["M" x1 y1
                   "L" x2 y2])))

(defmethod tool.hierarchy/render-edit :line
  [el]
  (let [offset (element/offset el)
        {{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])
        [x1 y1] (mat/add offset [x1 y1])
        [x2 y2] (mat/add offset [x2 y2])]
    [:g
     {:key ::edit-handles}
     (map (fn [handle]
            ^{:key (:id handle)}
            [overlay/square-handle handle
             [:title {:key (str (:id handle) "-title")} (name (:id handle))]])
          [{:x x1
            :y y1
            :id :starting-point
            :type :handle
            :tag :edit
            :element (:id el)}
           {:x x2
            :y y2
            :id :ending-point
            :type :handle
            :tag :edit
            :element (:id el)}])]))

(defmethod tool.hierarchy/edit :line
  [el [x y] handle]
  (case handle
    :starting-point
    (-> el
        (hierarchy/update-attr :x1 + x)
        (hierarchy/update-attr :y1 + y))

    :ending-point
    (-> el
        (hierarchy/update-attr :x2 + x)
        (hierarchy/update-attr :y2 + y))
    el))

(defmethod tool.hierarchy/bounds :line
  [el]
  (let [{{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
    [(min x1 x2) (min y1 y2) (max x1 x2) (max y1 y2)]))
