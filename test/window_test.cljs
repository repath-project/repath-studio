(ns window-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]
   [renderer.window.events :as e]
   [renderer.window.subs :as s]))

(deftest maximize
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/set-maximized false])
   (is (not @(rf/subscribe [::s/maximized])))

   (rf/dispatch [::e/set-maximized true])
   (is @(rf/subscribe [::s/maximized]))))

(deftest fullscreen
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/set-fullscreen false])
   (is (not @(rf/subscribe [::s/fullscreen])))

   (rf/dispatch [::e/set-fullscreen true])
   (is @(rf/subscribe [::s/fullscreen]))))
