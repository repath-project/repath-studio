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

(defn state->d3-data
  [history id save]
  (let [states (:states history)
        {:keys [index] :as state} (get states id)
        n (count states)
        explanation (:explanation state)]
    #js {:name (if (string? explanation) explanation (explanation))
         :id (str id)
         :saved (= id save)
         :active (= id (:position history))
         :color (str "hsla(" (+ (* (/ 100 n) index) 20) ",40%,60%,1)")
         :children (->> (:children state)
                        (map #(state->d3-data history % save))
                        (apply array))}))

(rf/reg-sub
 ::tree-data
 :<- [::history]
 :<- [::document.subs/save]
 (fn [[history save] _]
   (let [root (:id (first (sort-by :index (vals (:states history)))))]
     (state->d3-data history root save))))
