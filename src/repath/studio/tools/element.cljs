(ns repath.studio.tools.element
  (:require
   [repath.studio.tools.base :as tools]
   [repath.studio.handlers :as handlers]
   [repath.studio.elements.handlers :as element-handlers]
   [repath.studio.elements.views :as element-views]
   [reagent.core :as ra]
   [re-frame.core :as rf]
   [repath.studio.history.handlers :as history]
   [repath.studio.units :as units]
   [clojure.core.matrix :as matrix]
   [clojure.string :as str]
   [repath.studio.mouse :as mouse]
   [reagent.dom :as dom]))

(defmethod tools/activate ::tools/element [db] (assoc db
                                                      :cursor "crosshair"
                                                      :state :create))

(defmethod tools/translate ::tools/element
  [element [x y]] (-> element
                      (update-in [:attrs :x] #(units/transform + x %))
                      (update-in [:attrs :y] #(units/transform + y %))))

(defmethod tools/scale ::tools/element
  [element [x y] handler]
  (cond-> element
    (contains? #{:bottom-right
                 :top-right
                 :middle-right} handler) (update-in [:attrs :width] #(units/transform + x %))
    (contains? #{:bottom-left
                 :top-left
                 :middle-left} handler) (-> (update-in [:attrs :x] #(units/transform + x %))
                                            (update-in [:attrs :width] #(units/transform - x %)))
    (contains? #{:bottom-middle
                 :bottom-right
                 :bottom-left} handler) (update-in [:attrs :height] #(units/transform + y %))
    (contains? #{:top-middle
                 :top-left
                 :top-right} handler) (-> (update-in [:attrs :y] #(units/transform + y %))
                                          (update-in [:attrs :height] #(units/transform - y %)))))

(defmethod tools/bounds ::tools/element
    [{:keys [attrs]}]
    (let [{:keys [x y width height stroke-width stroke]} attrs
          [x y width height stroke-width-px] (mapv units/unit->px [x y width height stroke-width])
          stroke-width-px (if (str/blank? stroke-width) 1 stroke-width-px)
          [x y] (matrix/sub [x y] (/ (if (str/blank? stroke) 0 stroke-width-px) 2))
          [width height] (matrix/add [width height] (if (str/blank? stroke) 0 stroke-width-px))]
      (mapv units/unit->px [x y (+ x width) (+ y height)])))

(defmethod tools/drag-end ::tools/element
  [{:keys [state] :as db}]
  (let [temp-element (get-in db [:documents (:active-document db) :temp-element])]
    (cond-> db
      (= state :create) (-> (element-handlers/create-from-temp)
                            (history/finalize (str "Create " (name (:type temp-element)))))
      (= state :edit) (history/finalize (str "Edit "))
      :always (assoc :state :create))))

(defmethod tools/mouse-up :default
  [db event element]
  (cond-> db
    (= (:state db) :create) (handlers/set-tool (:type element)) 
    :always (element-handlers/select (some #(contains? (:modifiers event) %) #{:ctrl :shift}) element)))

(defn get-bounds
  "Experimental way of getting the bounds of uknown or complicated elements using the getBBox method.
   SEE https://developer.mozilla.org/en-US/docs/Web/API/SVGGraphicsElement/getBBox"
  [element-ref]
  (let [bounds (.getBBox element-ref #js {:stroke true})
        x (.-x bounds)
        y (.-y bounds)]
    [x y (+ x (.-width bounds)) (+ y (.-height bounds))]))

(defn update-bounds
  "We update the bounds on render. As a result, they lag behind the actual element.
   Wrapping the dispatch in requestAnimationFrame helps, but doesn't fix the problem.
   SEE https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame"
  [key element-ref]
  (.requestAnimationFrame js/window #(rf/dispatch [:elements/set-property key :bounds (get-bounds (dom/dom-node element-ref)) false])))

(defn render-to-dom
  "We need a reagent form-3 component in order to set the style attribute manually.
   React expects a map, but we need to set a string to avoid serializing css.
   We also experimentally calculate the bounds on updade."
  [{:keys [key attrs type title] :as element} child-elements]
  (ra/create-class
   {:display-name  "element-renderer"

    :componet-did-mount
    (fn
      [this]
      (update-bounds key this)
      (when (not-empty (:style attrs)) (.setAttribute (dom/dom-node this) "style" (:style attrs))))

    :component-did-update
    (fn
      [this _]
      (let [new-argv (second (ra/argv this))
            style (:style (into {} (:attrs (into {} new-argv))))]
        (update-bounds key this)
        (if (empty? style) (.removeAttribute (dom/dom-node this) "style") (.setAttribute (dom/dom-node this) "style" style))))

    :reagent-render
    (fn
      [{:keys [key attrs type title] :as element} child-elements]
      [type (merge (dissoc attrs :style) {:on-double-click #(mouse/event-handler % element)
                                          :on-mouse-up     #(mouse/event-handler % element)
                                          :on-mouse-down   #(mouse/event-handler % element)
                                          :on-mouse-move   #(mouse/event-handler % element)})
       (when title [:title title])
       (:content attrs)
       (map (fn [child] ^{:key (:key child)} [tools/render child]) child-elements)])}))

(defmethod tools/render ::tools/element
  [{:keys [children] :as element}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])]
    [render-to-dom element child-elements]))

(defmethod tools/edit ::tools/element
  [{:keys [attrs] :as element} zoom]
  (let [{:keys [x y width height]} attrs
        [x y width height] (mapv units/unit->px [x y width height])
        handler-size (/ 8 zoom)
        stroke-width (/ 1 zoom)]
    [:g {:key :edit-handlers}
     (map element-views/edit-handler [{:x x :y y :size handler-size :stroke-width stroke-width :key :position}
                                      {:x (+ x width) :y (+ y height) :size handler-size :stroke-width stroke-width :key :size}])]))