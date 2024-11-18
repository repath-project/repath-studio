(ns renderer.utils.unit
  (:require
   [clojure.string :as str]
   [malli.core :as m]))

(m/=> ->key [:-> string? keyword?])
(defn ->key
  "Converts the string unit to a lower-cased keyword."
  [s]
  (keyword (str/lower-case s)))

(m/=> match [:-> string? string?])
(defn match
  [s]
  (second (re-matches #"[\d.\-\+]*\s*(.*)" s)))

(m/=> parse [:-> [:or string? number? nil?] [:tuple number? string?]])
(defn parse
  [v]
  (let [s (str/trim (str v))
        n (js/parseFloat s 10)
        unit (match s)]
    [(if (js/isNaN n) 0 n) unit]))
