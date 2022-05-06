(ns repath.studio.handlers)

(defn set-state
  [db state]
  (assoc db
         :state state
         :cursor (if (= state :clone) "copy" "default")))