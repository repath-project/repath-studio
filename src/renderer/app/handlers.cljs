(ns renderer.app.handlers
  (:require
   [malli.experimental :as mx]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.hiccup :refer [Hiccup]]))

(mx/defn set-state
  [db, state :- keyword?]
  (assoc db :state state))

(mx/defn set-cursor
  [db, cursor :- string?]
  (assoc db :cursor cursor))

(mx/defn set-message
  [db, message :- Hiccup]
  (assoc db :message message))

(defn explain
  [db & rest]
  (assoc db :explanation (apply str rest)))

(defn add-fx
  [db effect]
  (update db :fx conj effect))

(mx/defn set-tool
  [db, tool :- keyword?]
  (-> db
      (tool.hierarchy/deactivate)
      (assoc :tool tool)
      (tool.hierarchy/activate)))

