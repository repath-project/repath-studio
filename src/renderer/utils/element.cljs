(ns renderer.utils.element
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   [clojure.core.matrix :as mat]
   [renderer.tool.base :as tool]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.map :as map]
   [renderer.utils.spec :as spec]))

(defn root?
  [el]
  (= :canvas (:tag el)))

(defn svg?
  [el]
  (= :svg (:tag el)))

(defn container?
  [el]
  #_(isa? (:tag el) ::tool/container)
  (or (svg? el) (root? el))) ; FIXME

(defn supported?
  [el]
  (and (map? el)
       (keyword? (:tag el))
       (contains? (descendants ::tool/element) (:tag el))))

(defn parent-container
  [elements el]
  (loop [parent (:parent el)]
    (when-let [parent-element (get elements parent)]
      (if (container? parent-element)
        parent-element
        (recur (:parent parent-element))))))

(defn adjusted-bounds
  [element elements]
  (when-let [bounds (tool/bounds element elements)]
    (if-let [container (parent-container elements element)]
      (let [[offset-x offset-y _ _] (tool/bounds container elements)
            [x1 y1 x2 y2] bounds]
        [(+ x1 offset-x) (+ y1 offset-y)
         (+ x2 offset-x) (+ y2 offset-y)])
      bounds)))

(defn bounds
  [elements]
  (let [bounds (->> elements
                    (map :bounds)
                    (remove nil?))]
    (when (seq bounds)
      (apply bounds/union bounds))))

(defn offset
  [el]
  (let [bounds (:bounds el)
        local-bounds (tool/bounds el)]
    (take 2 (mat/sub bounds local-bounds))))

(defn snapping-points
  [element options]
  (let [[x1 y1 x2 y2] (:bounds element)
        [cx cy] (bounds/center [x1 y1 x2 y2])]
    (concat (when (:corners options)
              [[x1 y1]
               [x1 y2]
               [x2 y1]
               [x2 y2]])

            (when (:centers options)
              [[cx cy]])

            (when (:midpoints options)
              [[x1 cy]
               [x2 cy]
               [cx y1]
               [cx y2]])

            (when :nodes
              (tool/snapping-points element)))))

(defn- attrs-map
  [attrs]
  (let [deprecated-path [:__compat :status :deprecated]
        filtered-attrs (->> attrs
                            (filter #(not (get-in (val %) deprecated-path)))
                            (into {}))]
    (-> filtered-attrs
        (dissoc :__compat :lang :tabindex)
        keys
        (zipmap (repeat "")))))

(defn attributes
  [{:keys [tag attrs]}]
  (merge
   (when tag
     (merge (when (isa? tag ::tool/element)
              (merge
               (attrs-map (tag (:elements spec/svg)))
               (attrs-map (-> spec/svg :attributes :core))
               (attrs-map (-> spec/svg :attributes :style))))
            (when (contains? #{:animateMotion :animateTransform} tag)
              (attrs-map (:animate (:elements spec/svg))))
            (zipmap (:attrs (tool/properties tag)) (repeat ""))))
   attrs))

(defn supports-attr?
  [el k]
  (-> el attributes k))

(defn ->path
  [el]
  (-> el
      (assoc :attrs (attributes
                     {:tag :path
                      :attrs (map/merge-common-with
                              str
                              (:attrs el)
                              (attributes {:tag :path
                                           :attrs {}}))})
             :tag :path)
      (assoc-in [:attrs :d] (tool/path el))))

(defn stroke->path
  [{:keys [attrs] :as el}]
  (let [d (tool/path el)
        paper-path (Path. d)
        offset (or (:stroke-width attrs) 1)
        stroke-path (PaperOffset.offsetStroke
                     paper-path
                     (/ offset 2)
                     #js {:cap (or (:stroke-linecap attrs) "butt")
                          :join (or (:stroke-linejoin attrs) "miter")})
        new-d (.getAttribute (.exportSVG stroke-path) "d")]
    (-> el
        (assoc :attrs (attributes {:tag :path
                                   :attrs (map/merge-common-with
                                           str
                                           (dissoc (:attrs el)
                                                   :stroke
                                                   :stroke-width)
                                           (attributes {:tag :path
                                                        :attrs {}}))})
               :tag :path)
        (assoc-in [:attrs :d] new-d)
        (assoc-in [:attrs :fill] (:stroke attrs)))))
