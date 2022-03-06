(ns repath.studio.tools.base
  (:require
   [re-frame.core :as rf]
   [repath.studio.helpers :as helpers]
   ["element-to-path" :as element-to-path]))

(derive ::transform ::tool)
(derive ::element ::tool)
(derive ::draw ::tool)
(derive ::edit ::tool)

(derive ::container ::element)
(derive ::renderable ::element)
(derive ::graphics ::renderable)
(derive ::shape ::graphics)
(derive ::gradient ::element)
(derive ::descriptive ::element)

(derive :a ::container)
(derive :clipPath ::container)
(derive :defs ::container)
(derive :marker ::container)
(derive :mask ::container)
(derive :pattern ::container)
(derive :switch ::container)
(derive :symbol ::container)

(derive :foreignObject ::graphics)
(derive :image ::graphics)
(derive :textPath ::graphics)
(derive :tspan ::graphics)

(derive :linearGradient ::gradient)
(derive :radialGradient ::gradient)

(derive :desc ::descriptive)
(derive :metadata ::descriptive)
(derive :title ::descriptive)

(derive :brush ::draw)
(derive :pen ::draw)

(defmulti render :type)
(defmulti attrs (fn [type _] type))
(defmulti properties (fn [type _] type))
(defmulti path :type)
(defmulti area :type)
(defmulti bounds (fn [_ element] (:type element)))

(defmulti mouse-move (fn [db _ _ _] (:tool db)))
(defmulti click (fn [db _ _ _] (:tool db)))
(defmulti drag (fn [db _ _ _] (:tool db)))
(defmulti drag-end (fn [db _ _ _] (:tool db)))
(defmulti activate :tool)
(defmulti deactivate :tool)

(defmulti move
  (fn [element _]
    (when (not (:locked? element)) (:type element))))

(defmulti scale
  (fn [element _ _]
    (when (not (:locked? element)) (:type element))))

(defmethod mouse-move :default [db _ _] db)
(defmethod drag :default [db _ _ _] db)
(defmethod drag-end :default [])
(defmethod properties :default [])
(defmethod render :default [])
(defmethod bounds :default [])
(defmethod area :default [])

(defmethod activate :default [db] db)
(defmethod deactivate :default [db] db)

(defmethod activate ::transform [db] (assoc db :cursor "default"))

(defmethod attrs :default [])
(defmethod scale :default [element _] element)
(defmethod move :default [element _] element)

(defn adjusted-bounds
  [elements element]
  (let [page (helpers/parent-page elements element)]
    (if page
      (let [[offset-x offset-y _ _] (bounds elements page)
            [x1 y1 x2 y2] (bounds elements element)]
        [(+ x1 offset-x) (+ y1 offset-y) (+ x2 offset-x) (+ y2 offset-y)])
      (bounds elements element))))

(defn merge-bounds
  [[ax1 ay1 ax2 ay2] [bx1 by1 bx2 by2]]
  [(min ax1 bx1) (min ay1 by1) (max ax2 bx2) (max ay2 by2)])

(defn elements-bounds
  [elements bound-elements]
  (reduce #(merge-bounds % (adjusted-bounds elements %2)) (adjusted-bounds elements (first bound-elements)) (rest bound-elements)))

(defmethod bounds ::container
  [elements element]
  (let [children (vals (select-keys elements (:children element)))]
    (elements-bounds elements children)))

(defmethod path ::shape
  [{:keys [attrs type]}]
  (element-to-path (clj->js {:name type :attributes attrs})))

(defn rgba [colors]
  (str "rgba(" (reduce str (interpose ", " colors)) ")"))

(defmethod render ::animation
  [{:keys [children type attrs]}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])]
    [type attrs (map (fn [element] ^{:key (:key element)} [render element]) child-elements)]))

(defmethod render ::container
  [{:keys [children type attrs]}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])]
    [type attrs (map (fn [element] ^{:key (:key element)} [render element]) child-elements)]))

(def svg-elements (js->clj js/window.api.bcd.svg.elements :keywordize-keys true))
(def svg-attributes (js->clj js/window.api.bcd.svg.attributes :keywordize-keys true))

(defn attrs-map
  [attrs]
  (let [filtered-attrs (into {} (filter #(not (get-in (val %) [:__compat :status :deprecated])) attrs))]
    (-> filtered-attrs
        (dissoc :__compat :lang :tabindex)
        (keys)
        (zipmap (repeat "")))))

(defn attributes
  [{:keys [type attrs]}]
  (merge (when type (merge (when (or (isa? type ::element) (= type :svg)) (merge (attrs-map (type svg-elements)) (attrs-map (:core svg-attributes)) (attrs-map (:style svg-attributes))))
                           (when (contains? #{:animateMotion :animateTransform} type) (attrs-map (:elements/animate svg-elements)))
                           (zipmap (:attrs (properties type)) (repeat "")))) attrs))

(defn to-path
  [element]
  (if (:locked? element)
    element
    (-> element
        (assoc :attrs (attributes {:type :path :attrs (helpers/merge-common str (:attrs element) (attributes {:type :path :attrs {}}))}))
        (assoc :type :path)
        (assoc-in [:attrs :d] (path element)))))
