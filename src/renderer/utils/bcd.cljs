(ns renderer.utils.bcd
  "Use BCD to get compatibility data for properties and more.
   https://github.com/mdn/browser-compat-data"
  (:require ["@mdn/browser-compat-data" :as bcd]))

(def svg
  (js->clj (.-svg bcd) :keywordize-keys true))

(defn conmpatibility
  "Returns conmpatibility data for tags or attributes."
  ([tag]
   (-> svg :elements tag :__compat))
  ([tag attr]
   (or (-> svg :elements tag attr :__compat)
       (-> svg :global_attributes attr :__compat))))
