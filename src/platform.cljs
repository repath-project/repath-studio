(ns platform
  (:require
   [clojure.string :as str]))

(defonce user-agent
  (.-userAgent js/navigator))

(defonce electron?
  (str/includes? user-agent "Electron"))

(defonce mac?
  (= js/window.api.platform "darwin"))

(defonce windows?
  (= js/window.api.platform "win32"))

(defonce linux?
  (= js/window.api.platform "linux"))