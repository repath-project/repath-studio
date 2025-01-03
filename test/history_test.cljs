(ns history-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.document.events :as-alias document.e]
   [renderer.element.events :as-alias element.e]
   [renderer.element.subs :as-alias element.s]
   [renderer.history.events :as-alias e]
   [renderer.history.subs :as-alias s]))

(deftest undo-redo
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (testing "no undos/redos"
     (is (not @(rf/subscribe [::s/undos?])))
     (is (not @(rf/subscribe [::s/redos?]))))

   (testing "add action to history"
     (rf/dispatch [::element.e/add {:tag :rect
                                    :attrs {:x 100
                                            :y 100
                                            :width 100
                                            :height 100}}])
     (is @(rf/subscribe [::s/undos?]))
     (is (= (count @(rf/subscribe [::s/undos])) 1))
     (is (not @(rf/subscribe [::s/redos?])))
     (is (= (-> @(rf/subscribe [::element.s/selected]) first :tag) :rect)))

   (testing "undo"
     (rf/dispatch [::e/undo])
     (is @(rf/subscribe [::s/redos?]))
     (is (= (count @(rf/subscribe [::s/redos])) 1))
     (is (not @(rf/subscribe [::s/undos?])))
     (is (empty? @(rf/subscribe [::element.s/selected]))))

   (testing "redo"
     (rf/dispatch [::e/redo])
     (is @(rf/subscribe [::s/undos?]))
     (is (= (count @(rf/subscribe [::s/undos])) 1))
     (is (not @(rf/subscribe [::s/redos?])))
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
     (is (not @(rf/subscribe [::s/undos?])))
     (is (not @(rf/subscribe [::s/redos?]))))))
