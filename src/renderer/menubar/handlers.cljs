(ns renderer.menubar.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.menubar.db :refer [Menu]]))

(m/=> activate [:-> App Menu App])
(defn activate
  ([db k]
   (-> db
       (assoc-in [:menubar :indicator] false)
       (assoc-in [:menubar :active] k)
       (assoc :backdrop true))))

(m/=> deactivate [:-> App App])
(defn deactivate
  ([db]
   (-> db
       (assoc-in [:menubar :indicator] false)
       (update :menubar dissoc :active)
       (assoc :backdrop false))))
