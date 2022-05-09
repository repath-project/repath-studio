(ns repath.studio.tools.base
  (:require
   [re-frame.core :as rf]
   [repath.studio.helpers :as helpers]
   [repath.studio.bounds :as bounds]
   ["element-to-path" :as element-to-path]
   [reagent.dom.server :as dom]))

(derive ::transform ::tool)
(derive ::element ::tool)
(derive ::draw ::tool)
(derive ::misc ::tool)

(derive ::container ::element)
(derive ::renderable ::element)
(derive ::graphics ::renderable)
(derive ::shape ::graphics)
(derive ::gradient ::element)
(derive ::descriptive ::element)

(derive ::animation ::descriptive)

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

(defmulti attrs keyword)
(defmulti properties keyword)

(defmulti render :type)
(defmulti render-to-string :type)
(defmulti path :type)
(defmulti area :type)

(defmulti render-edit #(:type %))
(defmulti bounds #(:type %))
(defmulti translate #(when (not (:locked? %)) (:type %)))
(defmulti scale #(when (not (:locked? %)) (:type %)))
(defmulti edit #(when (not (:locked? %)) (:type %)))

(defmulti mouse-down #(:tool %))
(defmulti mouse-move #(:tool %))
(defmulti mouse-up #(:tool %))
(defmulti double-click #(:tool %))
(defmulti drag #(:tool %))
(defmulti drag-start #(:tool %))
(defmulti drag-end #(:tool %))

(defmulti activate :tool)
(defmulti deactivate :tool)

(defn set-tool
  [db tool]
  (-> db
      (deactivate)
      (assoc :tool tool)
      (activate)))

(defmethod mouse-down :default [db] db)
(defmethod mouse-move :default [db] db)
(defmethod drag-start :default [db] db)
(defmethod double-click :default [db] db)

(defmethod drag :default [db event element] (mouse-move db event element))
(defmethod drag-end :default [db event element] (mouse-up db event element))
(defmethod properties :default [])
(defmethod render :default [])
(defmethod render-to-string :default [element] (dom/render-to-static-markup (render element)))
(defmethod render-edit :default [])
(defmethod bounds :default [])
(defmethod area :default [])

(defmethod activate :default [db] (assoc db :cursor "default"))
(defmethod deactivate :default [db] db)

(defmethod attrs :default [])
(defmethod scale :default [element] element)
(defmethod translate :default [element] element)

(defn adjusted-bounds
  [element elements]
  (let [page (helpers/parent-page elements element)]
    (if page
      (let [[offset-x offset-y _ _] (bounds page elements)
            [x1 y1 x2 y2] (bounds element elements)]
        [(+ x1 offset-x) (+ y1 offset-y) (+ x2 offset-x) (+ y2 offset-y)])
      (bounds element elements))))

(defn elements-bounds
  [elements bound-elements]
  (reduce #(bounds/union % (adjusted-bounds %2 elements)) (adjusted-bounds (first bound-elements) elements) (rest bound-elements)))

(defmethod bounds ::container
  [element elements]
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

(def svg-spec (js->clj js/window.api.bcd.svg :keywordize-keys true))

(defn attrs-map
  [attrs]
  (let [filtered-attrs (into {} (filter #(not (get-in (val %) [:__compat :status :deprecated])) attrs))]
    (-> filtered-attrs
        (dissoc :__compat :lang :tabindex)
        (keys)
        (zipmap (repeat "")))))

(defn attributes
  [{:keys [type attrs]}]
  (merge (when type (merge (when (or (isa? type ::element) (= type :svg)) (merge (attrs-map (type (:elements svg-spec))) (attrs-map (-> svg-spec :attributes :core)) (attrs-map (-> svg-spec :attributes :style))))
                           (when (contains? #{:animateMotion :animateTransform} type) (attrs-map (:animate (:elements svg-spec))))
                           (zipmap (:attrs (properties type)) (repeat "")))) attrs))

(defn to-path
  [element]
  (if (:locked? element)
    element
    (-> element
        (assoc :attrs (attributes {:type :path :attrs (helpers/merge-common str (:attrs element) (attributes {:type :path :attrs {}}))})
               :type :path)
        (assoc-in [:attrs :d] (path element)))))
