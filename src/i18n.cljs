(ns i18n
  "Internationalization namespace
   SEE https://github.com/taoensso/tempura"
  (:require [re-frame.core :as rf]
            [taoensso.tempura
             :refer [tr]
             :refer-macros [load-resource-at-compile-time]]))

(def dictionary
  "We need to load resources at compile time in clojurescript
   SEE https://github.com/taoensso/tempura/issues/25#issuecomment-451742526"
  {:en-US (load-resource-at-compile-time "translations/en-US.edn")
   :el-GR (load-resource-at-compile-time "translations/el-GR.edn")})

(def opts {:dict dictionary})

(defn t
  [resource-ids]
  (tr opts [@(rf/subscribe [:lang])] resource-ids))