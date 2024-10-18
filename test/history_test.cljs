(ns history-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]
   [renderer.document.events :as document.e]
   [renderer.element.events :as element.e]
   [renderer.element.subs :as element.s]
   [renderer.history.events :as e]
   [renderer.history.subs :as s]))

(deftest init
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (is (not @(rf/subscribe [::s/some-undos])))
   (is (not @(rf/subscribe [::s/some-redos])))))

(deftest add-actions
  (rf-test/run-test-sync))

(deftest undo-redo
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (rf/dispatch [::element.e/add {:tag :rect
                                  :attrs {:x 100 :y 100 :width 100 :height 100}}])

   (is @(rf/subscribe [::s/some-undos]))
   (is (= (count @(rf/subscribe [::s/undos])) 1))
   (is (not @(rf/subscribe [::s/some-redos])))
   (is (= (-> @(rf/subscribe [::element.s/selected]) first :tag) :rect))

   (rf/dispatch [::e/undo])
   (is @(rf/subscribe [::s/some-redos]))
   (is (= (count @(rf/subscribe [::s/redos])) 1))
   (is (not @(rf/subscribe [::s/some-undos])))
   (is (empty? @(rf/subscribe [::element.s/selected])))

   (rf/dispatch [::e/redo])
   (is @(rf/subscribe [::s/some-undos]))
   (is (= (count @(rf/subscribe [::s/undos])) 1))
   (is (not @(rf/subscribe [::s/some-redos])))
   (is (= (-> @(rf/subscribe [::element.s/selected]) first :tag) :rect))))


(deftest clear
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (rf/dispatch [::element.e/add {:tag :rect
                                  :attrs {:x 100 :y 100 :width 100 :height 100}}])
   (rf/dispatch [::e/clear])
   (is (not @(rf/subscribe [::s/some-undos])))
   (is (not @(rf/subscribe [::s/some-redos])))))
