(ns history-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.events :as-alias document.events]
   [renderer.element.events :as-alias element.events]
   [renderer.element.subs :as-alias element.subs]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as history.subs]))

(deftest undo-redo
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])
   (rf/dispatch [::document.events/init])

   (testing "no undos/redos"
     (is (not @(rf/subscribe [::history.subs/undos?])))
     (is (not @(rf/subscribe [::history.subs/redos?]))))

   (testing "add action to history"
     (rf/dispatch [::element.events/add {:tag :rect
                                         :attrs {:x 100
                                                 :y 100
                                                 :width 100
                                                 :height 100}}])
     (is @(rf/subscribe [::history.subs/undos?]))
     (is (= (count @(rf/subscribe [::history.subs/undos])) 1))
     (is (not @(rf/subscribe [::history.subs/redos?])))
     (is (= (-> @(rf/subscribe [::element.subs/selected]) first :tag) :rect)))

   (testing "undo"
     (rf/dispatch [::history.events/undo])
     (is @(rf/subscribe [::history.subs/redos?]))
     (is (= (count @(rf/subscribe [::history.subs/redos])) 1))
     (is (not @(rf/subscribe [::history.subs/undos?])))
     (is (empty? @(rf/subscribe [::element.subs/selected]))))

   (testing "redo"
     (rf/dispatch [::history.events/redo])
     (is @(rf/subscribe [::history.subs/undos?]))
     (is (= (count @(rf/subscribe [::history.subs/undos])) 1))
     (is (not @(rf/subscribe [::history.subs/redos?])))
     (is (= (-> @(rf/subscribe [::element.subs/selected]) first :tag) :rect)))))

(deftest clear
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])
   (rf/dispatch [::document.events/init])
   (rf/dispatch [::element.events/add {:tag :rect
                                       :attrs {:x 100
                                               :y 100
                                               :width 100
                                               :height 100}}])
   (testing "clear history"
     (rf/dispatch [::history.events/clear])
     (is (not @(rf/subscribe [::history.subs/undos?])))
     (is (not @(rf/subscribe [::history.subs/redos?]))))))
