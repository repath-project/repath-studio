(ns history-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.element.events :as-alias element.events]
   [renderer.element.subs :as-alias element.subs]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]))

(deftest history
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [undos? (rf/subscribe [::history.subs/undos?])
         redos? (rf/subscribe [::history.subs/redos?])
         undos (rf/subscribe [::history.subs/undos])
         redos (rf/subscribe [::history.subs/redos])
         selected-elements (rf/subscribe [::element.subs/selected])]

     (testing "no undos/redos"
       (is (not @undos?))
       (is (not @redos?)))

     (testing "add action to history"
       (rf/dispatch [::element.events/add {:tag :rect
                                           :attrs {:x 100
                                                   :y 100
                                                   :width 100
                                                   :height 100}}])
       (is @undos?)
       (is (= (count @undos) 1))
       (is (not @redos?))
       (is (= (-> @selected-elements first :tag) :rect)))

     (testing "undo"
       (rf/dispatch [::history.events/undo])
       (is @redos?)
       (is (= (count @redos) 1))
       (is (not @undos?))
       (is (empty? @selected-elements)))

     (testing "redo"
       (rf/dispatch [::history.events/redo])
       (is @undos?)
       (is (= (count @undos) 1))
       (is (not @redos?))
       (is (= (-> @selected-elements first :tag) :rect)))

     (testing "clear history"
       (rf/dispatch [::history.events/clear])
       (is (not @undos?))
       (is (not @redos?))))))
