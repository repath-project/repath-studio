(ns renderer.utils.units
  (:require
   [clojure.string :as str]
   [malli.experimental :as mx]))

(def ppi 96)

(def unit-to-pixel-map
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

(mx/defn unit->key :- keyword?
  "Converts the string unit to a lower-cased keyword."
  [s :- string?]
  (keyword (str/lower-case s)))

(mx/defn valid-unit? :- boolean?
  [s :- string?]
  (contains? unit-to-pixel-map (unit->key s)))

(mx/defn multiplier :- number?
  "Returns the multiplier by unit.
   If the unit is invalid, it fallbacks to :px (1)"
  [s :- string?]
  (get unit-to-pixel-map (if (valid-unit? s)
                           (unit->key s)
                           :px)))

(mx/defn match-unit :- string?
  [s :- string?]
  (second (re-matches #"[\d.\-\+]*\s*(.*)" s)))

(mx/defn parse-unit :- [:tuple number? string?]
  [v :- [:or string? number? nil?]]
  (let [s (str/trim (str v))
        n (js/parseFloat s 10)
        unit (match-unit s)]
    [(if (js/isNaN n) 0 n)
     unit]))

(mx/defn ->px :- number?
  [n :- number? unit :- string?]
  (* n (multiplier unit)))

(mx/defn ->unit :- number?
  [n :- number? unit :- string?]
  (/ n (multiplier unit)))

(mx/defn unit->px :- number?
  [v :- [:or string? number? nil?]]
  (let [[n unit] (parse-unit v)]
    (if (empty? unit)
      n
      (if (valid-unit? unit) (->px n unit) 0))))

(mx/defn transform :- string?
  "Converts a value to pixels, applies a function and converts the result
   back to the original unit."
  [v f :- fn? & more]
  (let [[n unit] (parse-unit v)]
    (-> (apply f (->px n unit) more)
        (.toFixed 2)
        (->unit unit)
        (str (when (valid-unit? unit) unit)))))
