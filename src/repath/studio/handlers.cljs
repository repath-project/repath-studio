(ns repath.studio.handlers
  (:require
   [repath.studio.tools.base :as tools]))

(defn set-tool
  [db tool]
  (-> db
      (tools/deactivate)
      (assoc :state :default)
      (assoc :tool tool)
      (tools/activate)))
