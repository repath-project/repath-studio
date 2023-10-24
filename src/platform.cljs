(ns platform
  (:require
   [clojure.string :as str])
  (:import
   [goog userAgent]))

(defonce user-agent
  (.getUserAgentString userAgent))

#_(defonce platform
    (.-PLATFORM userAgent))

#_(defonce mobile?
    (.-MOBILE userAgent))

(defonce electron?
  (str/includes? user-agent "Electron"))