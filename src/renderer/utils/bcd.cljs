(ns renderer.utils.bcd
  "Use BCD to get compatibility data for properties and more.
   https://github.com/mdn/browser-compat-data"
  (:require
   ["@mdn/browser-compat-data" :as bcd]
   [malli.core :as m]
   [renderer.element.db :as element.db :refer [Tag]]))

(def svg
  (js->clj (.-svg bcd) :keywordize-keys true))

(m/=> points->vec [:function
                   [:-> Tag map?]
                   [:-> Tag keyword? map?]])
(defn conmpatibility
  "Returns conmpatibility data for tags or attributes."
  ([tag]
   (-> svg :elements tag :__compat))
  ([tag attr]
   (or (-> svg :elements tag attr :__compat)
       (-> svg :global_attributes attr :__compat))))
