(ns document-test
  (:require
   [cljs.test :refer-macros [deftest are is]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]
   [renderer.document.db :as db]
   [renderer.document.events :as e]
   [renderer.document.subs :as s]))

(deftest init
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (is (db/valid? @(rf/subscribe [::s/active])))
   (are [v sub] (= v sub)
     "• Untitled-1 - Repath Studio" @(rf/subscribe [::s/title-bar])
     false @(rf/subscribe [::s/active-saved]))))
