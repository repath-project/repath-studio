(ns renderer.tool.impl.element.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement"
  (:require
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.length :as utils.length]))

(derive :svg ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :svg
  []
  {:icon "svg"})

(defmethod tool.hierarchy/help [:svg :create]
  []
  (t [::help [:div "Hold %1 to lock proportions."]]
     [[:span.shortcut-key "Ctrl"]]))

(defn attributes
  [db lock-ratio]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        width (utils.length/->fixed (abs (- x offset-x)))
        height (utils.length/->fixed (abs (- y offset-y)))]
    {:x (utils.length/->fixed (min x offset-x))
     :y (utils.length/->fixed (min y offset-y))
     :width (cond-> width lock-ratio (min height))
     :height (cond-> height lock-ratio (min width))}))

(defmethod tool.hierarchy/on-drag-start :svg
  [db e]
  (-> db
      (tool.handlers/set-state :create)
      (element.handlers/add {:tag :svg
                             :type :element
                             :attrs (attributes db (:ctrl-key e))})))

(defmethod tool.hierarchy/on-drag :svg
  [db e]
  (let [id (:id (first (element.handlers/selected db)))
        attrs (attributes db (:ctrl-key e))
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))]
    (element.handlers/update-el db id #(reduce assoc-attr % attrs))))

(defmethod tool.hierarchy/on-drag-end :svg
  [db _e]
  (-> db
      (history.handlers/finalize #(t [::create-svg "Create SVG"]))
      (tool.handlers/activate :transform)))
