(ns renderer.utils.spec
  (:require
   [malli.core :as m]
   [malli.error :as me]))

(defn explain
  [state spec]
  (->> state (m/explain spec) me/humanize str))
