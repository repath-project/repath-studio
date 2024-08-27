(ns renderer.utils.units
  (:require
   [clojure.string :as str]))

(def ppi 96)

(def units
  ;; TODO: Find an agnostix way to handle percentages (we need to pass a base).
  {:px 1
   :ch 8
   :ex 7.15625
   :em 16
   :rem 16
   :in ppi
   :cm (/ ppi 2.54)
   :mm (/ ppi 25.4)
   :pt (/ ppi 72)
   :pc (/ ppi 6)
   :% 1})

(defn unit->key
  "Converts the string unit to a lower-cased keyword."
  [s]
  (keyword (str/lower-case s)))

(defn valid-unit?
  [s]
  (contains? units (unit->key s)))

(defn multiplier
  "Returns the multiplier by unit.
   If the unit is invalid, it fallbacks to :px (1)"
  [s]
  (get units (if (valid-unit? s)
               (unit->key s)
               :px)))

(defn match-unit
  [s]
  (second (re-matches #"[\d.\-\+]*\s*(.*)" s)))

(defn parse-unit
  [s]
  (let [s (str/trim (str s))
        n (js/parseFloat s 10)
        unit (match-unit s)]
    [(if (js/isNaN n) 0 n)
     unit]))

(defn ->px
  [number unit]
  (* number (multiplier unit)))

(defn ->unit
  [number unit]
  (/ number (multiplier unit)))

(defn unit->px
  [v]
  (let [[n unit] (parse-unit v)]
    (if (empty? unit)
      n
      (if (valid-unit? unit) (->px n unit) 0))))

(defn ->fixed
  ([v]
   (->fixed v 2))
  ([v digits]
   (-> v
       (js/parseFloat)
       (.toFixed digits))))

(defn transform
  "Converts a value to pixels, applies a function and converts the result
   back to the original unit."
  [v f & more]
  (let [[n unit] (parse-unit v)]
    (-> (apply f (->px n unit) more)
        (->fixed)
        (->unit unit)
        (str (when (valid-unit? unit) unit)))))
