(ns renderer.tools.blob
  "Custom element for https://blobs.dev/"
  (:require
   ["blobs/v2" :as blobs]
   ["svg-path-bbox" :as svg-path-bbox]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as mat]
   [goog.math]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attr-hierarchy]
   [renderer.attribute.length :as length]
   [renderer.attribute.views :as attr-views]
   [renderer.components :as comp]
   [renderer.element.handlers :as elements]
   [renderer.overlay :as overlay]
   [renderer.tools.base :as tools]
   [renderer.utils.mouse :as mouse]
   [renderer.utils.units :as units]))

(derive ::blob ::tools/custom)

(derive ::x ::length/length)
(derive ::y ::length/length)
(derive ::size ::length/length)

(defmethod attr-hierarchy/form-element ::extraPoints
  [k v]
  [attr-views/range-input k v {:min 0
                               :max 50
                               :step "1"} 0])

(defmethod attr-hierarchy/form-element ::randomness
  [k v]
  [attr-views/range-input k v {:min 0
                               :max 50
                               :step "1"} 0])

(defmethod attr-hierarchy/form-element ::seed
  [k v disabled?]
  (let [random-seed (goog.math/randomInt 1000000)]
    [:<>
     [attr-views/form-input {:key k
                             :value v
                             :disabled? disabled?
                             :placeholder 0}]
     [:button.button.ml-px.inline-block.level-2.text-muted
      {:title "Generate random seed"
       :style {:flex "0 0 26px"
               :height "100%"}
       :on-click #(rf/dispatch [:element/set-attribute k random-seed])}
      [comp/icon "refresh"]]]))

(defmethod attr-hierarchy/description ::x
  []
  "Horizontal coordinate of the blob's center.")

(defmethod attr-hierarchy/description ::y
  []
  "Vertical coordinate of the blob's center.")

(defmethod attr-hierarchy/description ::seed
  []
  "A given seed will always produce the same blob.")

(defmethod attr-hierarchy/description ::extraPoints
  []
  "The actual number of points will be `3 + extraPoints`.")

(defmethod attr-hierarchy/description ::randomness
  []
  "Increases the amount of variation in point position.")

(defmethod attr-hierarchy/description ::size
  []
  "The size of the bounding box.")

(defmethod tools/properties ::blob
  []
  {:icon "blob"
   :description "Vector based blob."
   :url "https://blobs.dev/"
   :attrs [::x
           ::y
           ::seed
           ::extraPoints
           ::randomness
           ::size
           :fill
           :stroke
           :stroke-width
           :opacity]})

(defmethod tools/drag-start ::blob
  [{:keys [adjusted-mouse-offset
           active-document
           adjusted-mouse-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-mouse-offset
        radius (Math/sqrt (apply + (mat/pow
                                    (mat/sub adjusted-mouse-pos
                                             adjusted-mouse-offset)
                                    2)))
        attrs {::x (- offset-x radius)
               ::y (- offset-y radius)
               ::seed (goog.math/randomInt 1000000)
               ::extraPoints 8
               ::randomness 4
               ::size (* radius 2)
               :fill fill
               :stroke stroke}]
    (elements/set-temp db {:type :element :tag ::blob :attrs attrs})))

(defmethod tools/drag ::blob
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos] :as db}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        radius (Math/sqrt (apply + (mat/pow
                                    (mat/sub adjusted-mouse-pos
                                             adjusted-mouse-offset)
                                    2)))
        temp (-> (elements/get-temp db)
                 (assoc-in [:attrs ::x] (- offset-x radius))
                 (assoc-in [:attrs ::y] (- offset-y radius))
                 (assoc-in [:attrs ::size] (* radius 2)))]
    (elements/set-temp db temp)))


(defmethod tools/translate ::blob
  [element [x y]] (-> element
                      (attr-hierarchy/update-attr ::x + x)
                      (attr-hierarchy/update-attr ::y + y)))

(defmethod tools/scale ::blob
  [element [x y] handler]
  (cond-> element
    (contains? #{:middle-right} handler)
    (-> (attr-hierarchy/update-attr ::x + x)
        (attr-hierarchy/update-attr ::size + x))

    (contains? #{:middle-left} handler)
    (-> (attr-hierarchy/update-attr ::x + x)
        (attr-hierarchy/update-attr ::size - x))

    (contains? #{:bottom-middle} handler)
    (-> (attr-hierarchy/update-attr ::y + y)
        (attr-hierarchy/update-attr ::size + y))

    (contains? #{:top-middle} handler)
    (-> (attr-hierarchy/update-attr ::y + y)
        (attr-hierarchy/update-attr ::size - y))))

(defmethod tools/render ::blob
  [{:keys [attrs children] :as element}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        mouse-handler #(mouse/event-handler % element)]
    [:path (merge {:d (tools/path element)
                   :on-pointer-up mouse-handler
                   :on-pointer-down mouse-handler
                   :on-pointer-move mouse-handler}
                  (select-keys attrs [:stroke
                                      :fill
                                      :stroke-width
                                      :id
                                      :class
                                      :opacity])) child-elements]))

(defmethod tools/translate ::blob
  [element [x y]]
  (-> element
      (attr-hierarchy/update-attr ::x + x)
      (attr-hierarchy/update-attr ::y + y)))

(defmethod tools/bounds ::blob
  [element]
  (let [[left top right bottom] (js->clj (svg-path-bbox (tools/path element)))]
    [left top right bottom]))

(defmethod tools/centroid ::blob
  [{{:keys [::x ::y ::size]} :attrs}]
  (let [[x y size] (mapv units/unit->px [x y size])]
    (mat/add [x y] (/ size 2))))

(defmethod tools/path ::blob
  [{:keys [attrs]}]
  (let [options (reduce (fn [options [k v]] (assoc options k (int v)))
                        {}
                        (select-keys attrs [::seed
                                            ::extraPoints
                                            ::randomness
                                            ::size]))]
    (-> (.svgPath blobs (clj->js options))
        (svgpath)
        (.translate (units/unit->px (::x attrs)) (units/unit->px (::y attrs)))
        (.toString))))

(defmethod tools/edit ::blob
  [element [x y] handler]
  (case handler
    :size
    (attr-hierarchy/update-attr element ::size #(max 0 (+ % (min x y))))

    element))

(defmethod tools/render-edit ::blob
  [{:keys [attrs key] :as element}]
  (let [{:keys [::x ::y ::size]} attrs
        [x y size] (mapv units/unit->px [x y size])
        active-page @(rf/subscribe [:element/active-page])
        page-pos (mapv units/unit->px
                       [(-> active-page :attrs :x)
                        (-> active-page :attrs :y)])
        [x1 y1] (if (not= (:tag element) :page)
                  (mat/add page-pos [x y])
                  [x y])
        [x2 y2] (mat/add [x1 y1] size)]
    [:<>
     [overlay/line x1 y1 x2 y2]
     [overlay/square-handler
      {:type :handler
       :tag :edit
       :element key
       :x x2
       :y y2
       :key :size}]
     [overlay/times
      [x1 y1]]]))
