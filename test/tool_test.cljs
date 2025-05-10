(ns tool-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.events :as-alias document.events]
   [renderer.tool.events :as-alias e]
   [renderer.tool.subs :as-alias s]))

(deftest init
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])

   (is (= @(rf/subscribe [::s/active]) :transform))))

(deftest activate
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])
   (rf/dispatch [::document.events/init])

   (let [active-tool (rf/subscribe [::s/active])]
     (is (= @active-tool :transform))

     (rf/dispatch [::e/activate :rect])
     (is (= @active-tool :rect)))))
