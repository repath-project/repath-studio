(ns renderer.history.views
  #_(:require
     [clojure.zip :as zip]
     [re-frame.core :as rf]))

(defn select-options
  [history-list]
  (map-indexed (fn [index step] ^{:key (str "history-" index)} [:option {:key (str (:index step)) :value (inc index)} (str (:explanation step))]) history-list))


(defn tree
  []
  #_[:div {:style {:flex "0 0 300px"
                   :overflow "auto"}}
     (loop [step-count @(rf/subscribe [:history/step-count])]
       [:div.p-1
        step-count
        (loop [tree (zip/root @(rf/subscribe [:document/history]))]
          (map (fn [node] (if (and (zip/branch? (zip/vector-zip node)) (not (zip/end? node)))
                            [:div "dfdf"]
                            [:div {:style {:color (str "hsla(" (+ (* (/ 100 step-count) (:index (meta node))) 20) ",40%,60%,1)")}}
                             (:explanation (meta node)) (:index (meta node))])) tree))])])
