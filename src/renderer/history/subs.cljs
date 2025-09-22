(ns renderer.history.subs
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.history.handlers :as history.handlers]))

(rf/reg-sub
 ::history
 history.handlers/history)

(rf/reg-sub
 ::undos?
 :<- [::history]
 history.handlers/undos?)

(rf/reg-sub
 ::redos?
 :<- [::history]
 history.handlers/redos?)

(rf/reg-sub
 ::undos
 :<- [::history]
 history.handlers/undos)

(rf/reg-sub
 ::redos
 :<- [::history]
 history.handlers/redos)

(rf/reg-sub
 ::zoom
 :<- [::history]
 :-> :zoom)

(rf/reg-sub
 ::translate
 :<- [::history]
 :-> :translate)

(rf/reg-sub
 ::tree-data
 :<- [::history]
 :<- [::document.subs/saved-history-id]
 (fn [[history saved-history-id] _]
   (let [root-id (->> (:states history)
                      (vals)
                      (sort-by :index)
                      (first)
                      :id)]
     (history.handlers/state->d3-data history root-id saved-history-id))))
