(ns window-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.window.events :as-alias e]
   [renderer.window.subs :as-alias s]))

(deftest maximize
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])
   (rf/dispatch [::e/set-maximized false])
   (is (not @(rf/subscribe [::s/maximized?])))

   (rf/dispatch [::e/set-maximized true])
   (is @(rf/subscribe [::s/maximized?]))))

(deftest fullscreen
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])
   (rf/dispatch [::e/set-fullscreen false])
   (is (not @(rf/subscribe [::s/fullscreen?])))

   (rf/dispatch [::e/set-fullscreen true])
   (is @(rf/subscribe [::s/fullscreen?]))))
