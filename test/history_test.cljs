(ns history-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]
   [renderer.document.events :as document.e]
   [renderer.element.events :as element.e]
   [renderer.element.subs :as element.s]
   [renderer.history.events :as e]
   [renderer.history.subs :as s]))

(deftest undo-redo
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (testing "no undos/redos"
     (is (not @(rf/subscribe [::s/some-undos])))
     (is (not @(rf/subscribe [::s/some-redos]))))

   (testing "add action to history"
     (rf/dispatch [::element.e/add {:tag :rect
                                    :attrs {:x 100
                                            :y 100
                                            :width 100
                                            :height 100}}])
     (is @(rf/subscribe [::s/some-undos]))
     (is (= (count @(rf/subscribe [::s/undos])) 1))
     (is (not @(rf/subscribe [::s/some-redos])))
     (is (= (-> @(rf/subscribe [::element.s/selected]) first :tag) :rect)))

   (testing "undo"
     (rf/dispatch [::e/undo])
     (is @(rf/subscribe [::s/some-redos]))
     (is (= (count @(rf/subscribe [::s/redos])) 1))
     (is (not @(rf/subscribe [::s/some-undos])))
     (is (empty? @(rf/subscribe [::element.s/selected]))))

   (testing "redo"
     (rf/dispatch [::e/redo])
     (is @(rf/subscribe [::s/some-undos]))
     (is (= (count @(rf/subscribe [::s/undos])) 1))
     (is (not @(rf/subscribe [::s/some-redos])))
     (is (= (-> @(rf/subscribe [::element.s/selected]) first :tag) :rect)))))

(deftest clear
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (rf/dispatch [::element.e/add {:tag :rect
                                  :attrs {:x 100
                                          :y 100
                                          :width 100
                                          :height 100}}])
   (testing "clear history"
     (rf/dispatch [::e/clear])
     (is (not @(rf/subscribe [::s/some-undos])))
     (is (not @(rf/subscribe [::s/some-redos]))))))
