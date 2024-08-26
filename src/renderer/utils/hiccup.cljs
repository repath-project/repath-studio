(ns renderer.utils.hiccup
  (:require
   [clojure.zip :as zip]))

(def hiccup
  [:schema {:registry {"hiccup" [:orn
                                 [:node [:catn
                                         [:name keyword?]
                                         [:props [:? [:map-of keyword? any?]]]
                                         [:children [:* [:schema [:ref "hiccup"]]]]]]
                                 [:primitive [:orn
                                              [:nil nil?]
                                              [:boolean boolean?]
                                              [:number number?]
                                              [:text string?]]]]}}
   "hiccup"])

(defn find-svg
  [zipper]
  (loop [loc zipper]
    (if (zip/end? loc)
      (zip/root loc)
      (if (= (:tag (zip/node loc)) :svg)
        (zip/node loc)
        (recur (zip/next loc))))))

