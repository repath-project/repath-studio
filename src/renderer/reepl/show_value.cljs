(ns renderer.reepl.show-value
  (:require [cljs.pprint :as pprint]))

(defn pprint-str [val]
  (pprint/write val :stream nil))

(defn show-str [val]
  (if (string? val)
    val
    (pprint-str val)))

(defn show-value- [val config showers]
  (loop [shower-list showers]
    (if (empty? shower-list)
      (throw (js/Error. (str "No shower for value " val)))
      (let [res ((first shower-list) val config #(show-value- %1 %2 showers))]
        (if res
          [:div.inline-flex res]
          (recur (rest shower-list)))))))

(defn show-value [val opts show-opts]
  (show-value- val opts (conj (vec (:showers show-opts)) show-str)))
