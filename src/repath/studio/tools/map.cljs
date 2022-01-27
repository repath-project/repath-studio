(ns repath.studio.tools.map
  (:require [reagent.core :as ra]
            [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            [repath.studio.mouse :as mouse]
            [clojure.string :as string]))

(derive :map ::tools/element)

(defmethod tools/properties :map [] {:icon "map"
                                     :attrs [:lat
                                             :lng
                                             :zoom
                                             :x
                                             :y
                                             :width
                                             :height]})

(defmethod tools/drag :map
  [{:keys [adjusted-mouse-pos tool adjusted-mouse-offset]}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (Math/abs (- pos-x offset-x))
               :height (Math/abs (- pos-y offset-y))}]
    (rf/dispatch [:set-temp-element {:type tool
                                     :attrs attrs}])))


(defn ->href [image]
  (->> image
       (map char)
       (apply str)
       (js/btoa)
       (str "data:image/png;base64,")))

(defn render-map
  [{:keys [attrs] :as element} child-elements]
  (let [{:keys [key lat lng zoom]} attrs
        image (ra/atom nil)
        get-map (fn [a] (.then (js/window.api.osmsm (clj->js (merge {:center (when (and (:lat a) (:lng a)) (str (:lat a) "," (:lng a)))}
                                                                    (select-keys a [:width :height :zoom])))) #(reset! image (->href %))))]
    
    (ra/create-class             
     {:display-name  "my-component"      

      :component-did-mount            
      (fn [this]
        (get-map attrs)) 
      
      :component-did-update           
      (fn [this old-argv]          
        (let [new-argv (rest (ra/argv this))
              keys [:lat :lng :zoom :width :height]]
              ;; new-args (select-keys (into {} (:attrs (into {} new-argv))) keys)
              ;; old-args (select-keys (into {} (:attrs (into {} old-argv))) keys)]
          #_(when-not (= new-args old-args) (get-map attrs))))

      :reagent-render  
      (fn [{:keys [attrs] :as element} child-elements]     
        [:image (merge
                 {:href @image
                  :on-mouse-up   #(mouse/event-handler % element)
                  :on-mouse-down #(mouse/event-handler % element)
                  :on-mouse-move #(mouse/event-handler % element)}
                 (select-keys attrs [:x :y :width :height :id :class]))])})))

(defmethod tools/render :map
  [{:keys [children] :as element}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])]
    [render-map element child-elements]))
