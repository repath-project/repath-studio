(ns app-test
  (:require
   [cljs.test :refer-macros [deftest are]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as e]
   [renderer.app.subs :as s]))

(deftest init
  (rf-test/run-test-sync
   (rf/dispatch [::e/initialize-db])

   (are [v sub] (= v sub)
     :select @(rf/subscribe [::s/tool])
     {} @(rf/subscribe [::s/documents])
     [] @(rf/subscribe [::s/document-tabs]))))
