(ns renderer.utils.spec
  "Use BCD to get compatibility data for properties and more.
   https://github.com/mdn/browser-compat-data"
  (:require ["@mdn/browser-compat-data" :as bcd]))

(def svg
  (js->clj (.-svg bcd) :keywordize-keys true))

(def presentation-attrs
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/Presentation"
  (-> svg
      :attributes
      :presentation
      keys
      set))

(defn compat-data
  "Returns conmpatibility data for tags or attributes."
  ([tag]
   (-> svg :elements tag :__compat))
  ([tag attr]
   (or (-> svg :elements tag attr :__compat)
       (-> svg :attributes :presentation attr :__compat)
       (-> svg :attributes :core attr :__compat)
       (-> svg :attributes :style attr :__compat))))
