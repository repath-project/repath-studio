(ns window-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.window.events :as-alias e]
   [renderer.window.subs :as-alias s]))

(deftest window
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])

   (let [maximized (rf/subscribe [::s/maximized?])
         fullscreen (rf/subscribe [::s/fullscreen?])]

     (testing "maximize"
       (rf/dispatch [::e/set-maximized false])
       (is (not @maximized))

       (rf/dispatch [::e/set-maximized true])
       (is @maximized))

     (testing "fullscreen"
       (rf/dispatch [::e/set-fullscreen false])
       (is (not @fullscreen))

       (rf/dispatch [::e/set-fullscreen true])
       (is @fullscreen)))))
