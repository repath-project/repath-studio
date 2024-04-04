(ns renderer.utils.element
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   [renderer.tools.base :as tools]
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
  #_(isa? (:tag el) ::tools/container)
  (or (svg? el) (root? el))) ; FIXME

(defn parent-container
  [elements el]
  (loop [parent (:parent el)]
    (when-let [parent-element (get elements parent)]
      (if (container? parent-element)
        parent-element
        (recur (:parent parent-element))))))

(defn adjusted-bounds
  [element elements]
  (when-let [bounds (tools/bounds element elements)]
    (if-let [container (parent-container elements element)]
      (let [[offset-x offset-y _ _] (tools/bounds container elements)
            [x1 y1 x2 y2] bounds]
        [(+ x1 offset-x) (+ y1 offset-y)
         (+ x2 offset-x) (+ y2 offset-y)])
      bounds)))

(defn bounds
  [elements bound-elements]
  (let [bounds (->> bound-elements
                    (map #(adjusted-bounds % elements))
                    (remove nil?))]
    (when (seq bounds)
      (apply bounds/union bounds))))

(defn snapping-points
  [element elements]
  (let [[x1 y1 x2 y2] (adjusted-bounds element elements)
        [cx cy] (bounds/center [x1 y1 x2 y2])]
    [[x1 y1]
     [x1 y2]
     [x1 cy]
     [x2 y1]
     [x2 y2]
     [x2 cy]
     [cx y1]
     [cx y2]
     [cx cy]]))

(defn attrs-map
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
     (merge (when (isa? tag ::tools/element)
              (merge
               (attrs-map (tag (:elements spec/svg)))
               (attrs-map (-> spec/svg :attributes :core))
               (attrs-map (-> spec/svg :attributes :style))))
            (when (contains? #{:animateMotion :animateTransform} tag)
              (attrs-map (:animate (:elements spec/svg))))
            (zipmap (:attrs (tools/properties tag)) (repeat ""))))
   attrs))

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
      (assoc-in [:attrs :d] (tools/path el))))

(defn stroke->path
  [{:keys [attrs] :as el}]
  (let [d (tools/path el)
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
