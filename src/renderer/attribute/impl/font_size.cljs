(ns renderer.attribute.impl.font-size
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/font-size"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]))

(defmethod attribute.hierarchy/description [:default :font-size]
  []
  "The font-size attribute refers to the size of the font from baseline to
   baseline when multiple lines of text are set solid in a multiline layout environment.")

(defn get-font-size!
  [{:keys [content] :as el}]
  (when-let [svg (utils.dom/canvas-element!)]
    (let [dom-el (utils.element/->dom-element el)]
      (.appendChild svg dom-el)
      (set! (.-innerHTML dom-el) (if (empty? content) "\u00a0" content))
      (let [computed-style (.getComputedStyle js/window dom-el nil)
            font-size (.getPropertyValue computed-style "font-size")]
        (.remove dom-el)
        font-size))))

(defmethod attribute.hierarchy/update-attr :font-size
  [el attribute f & more]
  (let [font-size (get-font-size! el)
        font-size (utils.length/unit->px font-size)]
    (assoc-in el [:attrs attribute] (str (apply f font-size more)))))

(defonce sizes
  ["xx-small"
   "x-small"
   "small"
   "medium"
   "large"
   "x-large"
   "xx-large"
   "xxx-large"])

(defmethod attribute.hierarchy/form-element [:default :font-size]
  [_ k v attrs]
  [attribute.views/select-input k v
   (merge attrs
          {:default-value "medium"
           :items (mapv #(do {:key %
                              :label %
                              :value %}) sizes)})])
