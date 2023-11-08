(ns renderer.tools.container
  "https://www.w3.org/TR/SVG/struct.html#TermContainerElement"
  (:require
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]))

(derive ::tools/container ::tools/box)

(derive :a ::tools/container)
(derive :clipPath ::tools/container)
(derive :defs ::tools/container)
(derive :marker ::tools/container)
(derive :mask ::tools/container)
(derive :pattern ::tools/container)
(derive :switch ::tools/container)
(derive :symbol ::tools/container)

(defmethod tools/bounds ::tools/container
  [element elements]
  (let [children (vals (select-keys elements (:children element)))]
    (tools/elements-bounds elements children)))

(defmethod tools/render ::tools/container
  [{:keys [children tag attrs]}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])]
    [tag attrs (map (fn [element]
                      ^{:key (:key element)}
                      [tools/render element])
                    child-elements)]))

(defmethod tools/render-to-string ::tools/container
  [{:keys [tag attrs title children]}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])
        attrs (->> (dissoc attrs :style)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (-> [tag
         attrs
         (when title [:title title])
         (map tools/render-to-string child-elements)])))