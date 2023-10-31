(ns renderer.tools.base
  (:require
   [renderer.elements.utils :as el-utils]
   [renderer.utils.map :as map]
   [renderer.utils.bounds :as bounds]
   ["paperjs-offset" :refer [PaperOffset]]
   ["paper" :refer [Path]]
   ["@mdn/browser-compat-data" :as bcd]
   [reagent.dom.server :as server]
   [goog.string :as gstring]))

(derive ::transform ::tool)
(derive ::draw ::tool)
(derive ::misc ::tool)

(derive ::renderable ::element)
(derive ::never-renderable ::element)

(derive ::graphics ::renderable)

(derive ::gradient ::element)
(derive ::descriptive ::element)
(derive ::custom ::element)

(derive :foreignObject ::graphics)
(derive :textPath ::graphics)
(derive :tspan ::graphics)

(derive :linearGradient ::gradient)
(derive :radialGradient ::gradient)

(derive :desc ::descriptive)
(derive :metadata ::descriptive)
(derive :title ::descriptive)

(defmulti attrs keyword)
(defmulti properties keyword)

(defmulti render :tag)
(defmulti render-to-string :tag)
(defmulti path :tag)
(defmulti stroke->path :tag)
(defmulti area :tag)
(defmulti centroid :tag)
(defmulti poi :tag) ;; pole-of-inaccessibility

(defmulti render-edit #(:tag %))
(defmulti bounds #(:tag %))
(defmulti translate #(when-not (:locked? %) (:tag %)))
(defmulti scale #(when-not (:locked? %) (:tag %)))
(defmulti edit #(when-not (:locked? %) (:tag %)))

(defmulti mouse-down #(:tool %))
(defmulti mouse-move #(:tool %))
(defmulti mouse-up #(:tool %))
(defmulti double-click #(:tool %))
(defmulti drag #(:tool %))
(defmulti drag-start #(:tool %))
(defmulti drag-end #(:tool %))

(defmulti key-up #(:tool %))
(defmulti key-down #(:tool %))

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

(defmethod key-up :default [db] db)
(defmethod key-down :default [db] db)

(defmethod drag :default [db event element] (mouse-move db event element))
(defmethod drag-end :default [db event element] (mouse-up db event element))
(defmethod properties :default [])
(defmethod render :default [])
(defmethod render-to-string :default
  [element]
  (gstring/unescapeEntities
   (server/render-to-static-markup
    [render element])))

(defmethod render-edit :default [])
(defmethod bounds :default [])
(defmethod area :default [])
(defmethod centroid :default [])
(defmethod poi :default [])

(defmethod activate :default [db] (assoc db :cursor "default"))
(defmethod deactivate :default [db] db)

(defmethod attrs :default [])
(defmethod scale :default [element] element)
(defmethod translate :default [element] element)

(defn adjusted-bounds
  [element elements]
  (let [page (el-utils/parent-page elements element)]
    (if (not page)
      (bounds element elements)
      (let [[offset-x offset-y _ _] (bounds page elements)
            [x1 y1 x2 y2] (bounds element elements)]
        [(+ x1 offset-x) (+ y1 offset-y)
         (+ x2 offset-x) (+ y2 offset-y)]))))

(defn elements-bounds
  [elements bound-elements]
  (reduce
   #(bounds/union % (adjusted-bounds %2 elements))
   (adjusted-bounds (first bound-elements) elements) (rest bound-elements)))

(def svg-spec
  (js->clj (.-svg bcd) :keywordize-keys true))

(defn attrs-map
  [attrs]
  (let [deprecated-path [:__compat :status :deprecated]
        filtered-attrs (->> attrs
                            (filter #(not (get-in (val %) deprecated-path)))
                            (into {}))]
    (-> filtered-attrs
        (dissoc :__compat :lang :tabindex)
        (keys)
        (zipmap (repeat "")))))

(defn attributes
  [{:keys [tag attrs]}]
  (merge
   (when tag
     (merge (when (or (isa? tag ::shape) (= tag :svg))
              (merge
               (attrs-map (tag (:elements svg-spec)))
               (attrs-map (-> svg-spec :attributes :core))
               (attrs-map (-> svg-spec :attributes :style))))
            (when (contains? #{:animateMotion :animateTransform} tag)
              (attrs-map (:animate (:elements svg-spec))))
            (zipmap (:attrs (properties tag)) (repeat ""))))
   attrs))

(defn ->path
  [element]
  (if (:locked? element)
    element
    (-> element
        (assoc :attrs (attributes
                       {:tag :path
                        :attrs (map/merge-common-with
                                str
                                (:attrs element)
                                (attributes {:tag :path
                                             :attrs {}}))})
               :tag :path)
        (assoc-in [:attrs :d] (path element)))))

(defmethod stroke->path :default
  [{:keys [attrs] :as element}]
  (if (:locked? element)
    element
    (let [d (path element)
          paper-path (Path. d)
          offset (or (:stroke-width attrs) 1)
          stroke-path (PaperOffset.offsetStroke
                       paper-path
                       (/ offset 2)
                       #js {:cap (or (:stroke-linecap attrs) "butt")
                            :join (or (:stroke-linejoin attrs) "miter")})
          new-d (.getAttribute (.exportSVG stroke-path) "d")]
      (-> element
          (assoc :attrs (attributes {:tag :path
                                     :attrs (map/merge-common-with
                                             str
                                             (dissoc (:attrs element)
                                                     :stroke
                                                     :stroke-width)
                                             (attributes {:tag :path
                                                          :attrs {}}))})
                 :tag :path)
          (assoc-in [:attrs :d] new-d)
          (assoc-in [:attrs :fill] (:stroke attrs))))))