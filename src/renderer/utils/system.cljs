(ns renderer.utils.system
  (:require
   [clojure.string :as string]))

(defonce user-agent
  (.-userAgent js/navigator))

(defonce electron?
  (string/includes? user-agent "Electron"))

(defonce platform
  (when electron? js/window.api.platform))

(defonce mac?
  (= platform "darwin"))

#_(defonce windows?
    (= platform "win32"))

#_(defonce linux?
    (= platform "linux"))
