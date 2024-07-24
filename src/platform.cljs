(ns platform
  (:require
   [clojure.string :as str]))

(defonce user-agent
  (.-userAgent js/navigator))

(defonce electron?
  (str/includes? user-agent "Electron"))

(defonce mac?
  (and electron? (= js/window.api.platform "darwin")))

#_(defonce windows?
    (and electron? (= js/window.api.platform "win32")))

#_(defonce linux?
    (and electron? (= js/window.api.platform "linux")))
