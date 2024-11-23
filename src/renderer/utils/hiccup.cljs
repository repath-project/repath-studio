(ns renderer.utils.hiccup
  (:require
   [clojure.zip :as zip]))

(def Props [:? [:map-of keyword? any?]])

(def Hiccup
  [:schema {:registry {"hiccup" [:orn
                                 [:node [:catn
                                         [:name keyword?]
                                         [:props Props]
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
