(ns renderer.utils.units
  (:require
   [clojure.string :as str]))

(def ppi 96)

(def units {:px 1
            :ch 8
            :ex 7.15625
            :em 16
            :rem 16
            :in ppi
            :cm (/ ppi 2.54)
            :mm (/ ppi 25.4)
            :pt (/ ppi 72)
            :pc (/ ppi 6)
            ;; TODO Find an agnostix way to handle percentages
            :% 1})

(defn unit->key
  "Converts the string unit to a lower-cased keyword."
  [unit]
  (keyword (str/lower-case unit)))

(defn valid-unit?
  [unit]
  (contains? units (unit->key unit)))

(defn multiplier
  "Returns the multiplier by unit.
   If the unit is invalid, it fallbacks to :px (1)"
  [unit]
  ((if (valid-unit? unit) (unit->key unit) :px) units))

(defn match-unit
  [s]
  (second (re-matches #"[\d.\-\+]*\s*(.*)" s)))

(defn parse-unit
  [s]
  (let [string (str/trim (str s))
        number (js/parseFloat string 10)
        unit (match-unit string)]
    [(if (js/isNaN number)
       0
       number)
     unit]))

(defn ->px
  [number unit]
  (* number (multiplier unit)))

(defn ->unit
  [number unit]
  (/ number (multiplier unit)))

(defn unit->px
  [value]
  (let [[number unit] (parse-unit value)]
    (if (empty? unit)
      number
      (if (valid-unit? unit) (->px number unit) 0))))

(defn transform
  "Converts a value to pixels, applies a transformation and converts the result 
   back to the original unit."
  [transformation-f transformation-v value]
  (let [[number unit] (parse-unit value)]
    (-> number
        (->px unit)
        (transformation-f transformation-v)
        (js/parseFloat)
        (.toFixed 2)
        (->unit unit)
        (str (when (valid-unit? unit) unit)))))

(defn ->fixed
  ([value]
   (->fixed value 2))
  ([value digits]
   (-> value
       (.toFixed digits)
       (js/parseFloat))))
