(ns renderer.history.subs
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.history.handlers :as h]))

(rf/reg-sub
 ::history
 h/history)

(rf/reg-sub
 ::undos?
 :<- [::history]
 h/undos?)

(rf/reg-sub
 ::redos?
 :<- [::history]
 h/redos?)

(rf/reg-sub
 ::undos
 :<- [::history]
 h/undos)

(rf/reg-sub
 ::redos
 :<- [::history]
 h/redos)

(rf/reg-sub
 ::zoom
 :<- [::history]
 :-> :zoom)

(rf/reg-sub
 ::translate
 :<- [::history]
 :-> :translate)

(defn state->d3-data
  [history id save]
  (let [states (:states history)
        {:keys [index] :as state} (get states id)
        n (count states)]
    #js {:name (:explanation state)
         :id (str id)
         :saved (= id save)
         :active (= id (:position history))
         :color (str "hsla(" (+ (* (/ 100 n) index) 20) ",40%,60%,1)")
         :children (apply array (map #(state->d3-data history % save) (:children state)))}))

(rf/reg-sub
 ::tree-data
 :<- [::history]
 :<- [::document.s/save]
 (fn [[history save] _]
   (let [root (:id (first (sort-by :index (vals (:states history)))))]
     (state->d3-data history root save))))
