(ns platform
  (:require
   [clojure.string :as str]))

(defonce user-agent
  (.-userAgent js/navigator))

(defonce electron?
  (str/includes? user-agent "Electron"))
