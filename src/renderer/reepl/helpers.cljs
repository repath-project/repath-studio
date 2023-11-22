(ns renderer.reepl.helpers
  #_(:require [reagent.core :as r]))

(def text-style {:display "inline-block"
                 :flex-shrink 0
                 :box-sizing "border-box"})

(def view-style {:display "flex"
                 :flex-direction "column"
                 :flex-shrink 0
                 :box-sizing "border-box"})

(defn get-styles [styles style-prop]
  (cond
    (not style-prop) {}
    (keyword? style-prop) (styles style-prop)
    (sequential? style-prop) (reduce (fn [a b] (merge a (get-styles styles b))) {} style-prop)
    :else style-prop))

(defn parse-props [styles default-style props]
  (if (keyword? props)
    (parse-props styles default-style {:style props})
    (merge {:style (merge default-style (get-styles styles (:style props)))}
           (dissoc props :style)))
  #_(if (keyword? props)
      {:style (merge default-style (props styles))}
      (let [style-prop (:style props)
            style (if (keyword? style-prop)
                    (styles style-prop)
                    style)
            style (merge default-style style)
            props (merge {:style style} (dissoc props :style))]
        props)))

(defn better-el [dom-el default-style styles props & children]
  (let [[props children]
        (if (or (keyword? props) (map? props))
          [props children]
          [nil (concat [props] children)])]

    (vec (concat [dom-el (parse-props styles default-style props)] children))))

(def view (partial better-el :div view-style))
(def text (partial better-el :span text-style))
; TODO: have the button also stop-propagation
#_(defn hoverable [_config & _children]
    (let [hovered (r/atom false)]
      (fn [{:keys [style hover-style el props]}
           & children]
        (into
         [el (assoc props
                    :style (if @hovered
                             (merge style hover-style)
                             style)
                    :on-mouse-over (fn [] (reset! hovered true) nil)
                    :on-mouse-out (fn [] (reset! hovered false) nil))]

         children))))
