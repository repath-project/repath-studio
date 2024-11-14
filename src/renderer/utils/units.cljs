(ns renderer.utils.units
  (:require
   [clojure.string :as str]
   [malli.core :as m]))

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

(m/=> unit->key [:-> string? keyword?])
(defn unit->key
  "Converts the string unit to a lower-cased keyword."
  [s]
  (keyword (str/lower-case s)))

(m/=> valid-unit? [:-> string? boolean?])
(defn valid-unit?
  [s]
  (contains? unit-to-pixel-map (unit->key s)))

(m/=> multiplier [:-> string? number?])
(defn multiplier
  "Returns the multiplier by unit.
   If the unit is invalid, it fallbacks to :px (1)"
  [s]
  (get unit-to-pixel-map (if (valid-unit? s)
                           (unit->key s)
                           :px)))

(m/=> match-unit [:-> string? string?])
(defn match-unit
  [s]
  (second (re-matches #"[\d.\-\+]*\s*(.*)" s)))

(m/=> parse-unit [:-> [:or string? number? nil?] [:tuple number? string?]])
(defn parse-unit
  [v]
  (let [s (str/trim (str v))
        n (js/parseFloat s 10)
        unit (match-unit s)]
    [(if (js/isNaN n) 0 n) unit]))

(m/=> ->px [:-> number? string? number?])
(defn ->px
  [n unit]
  (* n (multiplier unit)))

(m/=> ->unit [:-> number? string? number?])
(defn ->unit
  [n unit]
  (/ n (multiplier unit)))

(m/=> unit->px [:-> [:or string? number? nil?] number?])
(defn unit->px
  [v]
  (let [[n unit] (parse-unit v)]
    (if (empty? unit)
      n
      (if (valid-unit? unit) (->px n unit) 0))))

(defn transform
  "Converts a value to pixels, applies a function and converts the result
   back to the original unit."
  ([v f & more]
   (let [[n unit] (parse-unit v)]
     (-> (apply f (->px n unit) more)
         (.toFixed 2)
         (js/parseFloat)
         (->unit unit)
         (str (when (valid-unit? unit) unit))))))
