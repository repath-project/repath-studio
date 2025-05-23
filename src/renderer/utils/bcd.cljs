(ns renderer.utils.bcd
  "Use BCD to get compatibility data for properties and more.
   https://github.com/mdn/browser-compat-data"
  (:require
   ["@mdn/browser-compat-data" :as bcd]
   [malli.core :as m]
   [renderer.element.db :refer [Tag]]))

(defonce svg
  (js->clj (.-svg bcd) :keywordize-keys true))

(m/=> compatibility [:function
                     [:-> Tag map?]
                     [:-> Tag keyword? map?]])
(defn compatibility
  "Returns compatibility data for tags or attributes."
  ([tag]
   (-> svg :elements tag :__compat))
  ([tag attr]
   (or (-> svg :elements tag attr :__compat)
       (-> svg :global_attributes attr :__compat))))
