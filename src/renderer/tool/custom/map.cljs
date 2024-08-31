(ns renderer.tool.custom.map
  (:require
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as element.h]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.base :as tool]
   [renderer.utils.pointer :as pointer]))

(derive :map ::tool/box)
(derive :map ::tool/custom)

(defmethod tool/properties :map
  []
  {:icon "map"
   :attrs [:lat
           :lng
           :zoom
           :x
           :y
           :width
           :height]})

(defmethod tool/drag :map
  [{:keys [adjusted-pointer-pos tool adjusted-pointer-offset] :as db}]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos]
    (element.h/set-temp db {:type tool
                            :attrs {:x (min pos-x offset-x)
                                    :y (min pos-y offset-y)
                                    :width (abs (- pos-x offset-x))
                                    :height (abs (- pos-y offset-y))}})))

(defn ->href [image]
  (->> image
       (map char)
       (apply str)
       (js/btoa)
       (str "data:image/png;base64,")))

(defn render-map
  [{:keys [attrs] :as _element} _child-elements]
  (let [{:keys [_key _lat _lng _zoom]} attrs
        image (ra/atom nil)
        get-map (fn [a]
                  (.then
                   (js/window.api.osmsm
                    (clj->js (merge {:center (when (and (:lat a) (:lng a)) (str (:lat a) "," (:lng a)))}
                                    (select-keys a [:width :height :zoom])))) #(reset! image (->href %))))]

    (ra/create-class
     {:display-name  "my-component"

      :component-did-mount
      (fn [_this]
        (get-map attrs))

      :component-did-update
      (fn [this _old-argv]
        (let [_new-argv (rest (ra/argv this))
              _keys [:lat :lng :zoom :width :height]]
              ;; new-args (select-keys (into {} (:attrs (into {} new-argv))) keys)
              ;; old-args (select-keys (into {} (:attrs (into {} old-argv))) keys)]
          #_(when-not (= new-args old-args) (get-map attrs))))

      :reagent-render
      (fn [{:keys [attrs] :as element} _child-elements]
        [:image (merge
                 {:href @image
                  :on-pointer-up   #(pointer/event-handler % element)
                  :on-pointer-down #(pointer/event-handler % element)
                  :on-pointer-move #(pointer/event-handler % element)}
                 (select-keys attrs [:x :y :width :height :id :class]))])})))

(defmethod tool/render :map
  [{:keys [children] :as element}]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible children])]
    [render-map element child-elements]))
