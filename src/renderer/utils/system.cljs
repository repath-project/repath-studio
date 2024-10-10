(ns renderer.utils.system
  (:require
   [clojure.string :as str]))

(defonce language
  (keyword (.-language js/navigator)))

(defonce user-agent
  (.-userAgent js/navigator))

(defonce electron?
  (str/includes? user-agent "Electron"))

(defonce platform
  (when electron? js/window.api.platform))

(defonce mac?
  (= platform "darwin"))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defonce windows?
  (= platform "win32"))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defonce linux?
  (= platform "linux"))
