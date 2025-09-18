(ns renderer.tool.impl.element.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.length :as utils.length]))

(derive :ellipse ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :ellipse
  []
  {:icon "ellipse-tool"
   :label (t [::label "Ellipse"])})

(defmethod tool.hierarchy/help [:ellipse :create]
  []
  (t [::help [:div "Hold %1 to lock proportions."]]
     [[:span.shortcut-key "Ctrl"]]))

(defn attributes
  [db lock-ratio]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        rx (abs (- x offset-x))
        ry (abs (- y offset-y))]
    {:rx (utils.length/->fixed (cond-> rx lock-ratio (min ry)))
     :ry (utils.length/->fixed (cond-> ry lock-ratio (min rx)))}))

(defmethod tool.hierarchy/on-drag-start :ellipse
  [db e]
  (let [[x y] (or (:nearest-neighbor-offset db)
                  (:adjusted-pointer-offset db))
        fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :ellipse
                               :attrs (merge (attributes db (:ctrl-key e))
                                             {:cx x
                                              :cy y
                                              :fill fill
                                              :stroke stroke})}))))

(defmethod tool.hierarchy/on-drag :ellipse
  [db e]
  (let [attrs (attributes db (:ctrl-key e))
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))
        id (:id (first (element.handlers/selected db)))]
    (element.handlers/update-el db id #(reduce assoc-attr % attrs))))

(defmethod tool.hierarchy/on-drag-end :ellipse
  [db _e]
  (-> db
      (history.handlers/finalize [::create-ellipse "Create ellipse"])
      (tool.handlers/activate :transform)))
