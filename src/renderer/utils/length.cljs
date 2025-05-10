(ns renderer.utils.length
  (:require
   [malli.core :as m]
   [renderer.utils.unit :as utils.unit]))

(def ppi 96)

(def units
  #{"px" "ch" "ex" "em" "rem" "in" "cm" "mm" "pt" "pc" "%"})

(def unit-to-pixel-map
  ;; TODO: Find an agnostic way to handle percentages (we need to pass a base).
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

(m/=> valid-unit? [:-> string? boolean?])
(defn valid-unit?
  [s]
  (contains? units s))

(m/=> multiplier [:-> string? number?])
(defn multiplier
  "Returns the multiplier by unit.
   If the unit is invalid, it fallbacks to :px (1)"
  [s]
  (get unit-to-pixel-map (if (valid-unit? s)
                           (utils.unit/->key s)
                           :px)))

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
  (let [[n unit] (utils.unit/parse v)]
    (if (empty? unit)
      n
      (if (valid-unit? unit) (->px n unit) 0))))

(m/=> transform [:-> [:or string? number? nil?] ifn? [:* any?] string?])
(defn transform
  "Converts a value to pixels, applies a function and converts the result
   back to the original unit."
  ([v f & more]
   (let [[n unit] (utils.unit/parse v)]
     (-> (apply f (->px n unit) more)
         (.toFixed 2)
         (js/parseFloat)
         (->unit unit)
         (str (when (valid-unit? unit) unit))))))
