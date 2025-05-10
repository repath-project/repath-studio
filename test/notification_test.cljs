(ns notification-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.notification.events :as-alias e]
   [renderer.notification.subs :as-alias s]))

(deftest add-and-remove
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])

   (let [notifications (rf/subscribe [::s/entities])]
     (testing "initial"
       (is (= @notifications [])))

     (testing "add"
       (rf/dispatch [::e/add [:div "test"]])
       (is (= (count @notifications) 1))
       (is (= (first @notifications) {:content [:div "test"]
                                      :count 1})))

     (testing "merge identical"
       (rf/dispatch [::e/add [:div "test"]])
       (is (= (count @notifications) 1))
       (is (= (first @notifications) {:content [:div "test"]
                                      :count 2})))

     (testing "add different"
       (rf/dispatch [::e/add [:div "test 2"]])
       (is (= (count @notifications) 2))
       (is (= (second @notifications) {:content [:div "test 2"]
                                       :count 1})))

     (testing "remove nth"
       (rf/dispatch [::e/remove-nth 1])
       (is (= (count @notifications) 1)))

     (testing "clear all"
       (rf/dispatch [::e/clear-all])
       (is (= (count @notifications) 0))))))

(deftest exception
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])

   (let [notifications (rf/subscribe [::s/entities])]
     (testing "string exception"
       (try
         (throw (js/Error. "Error message"))
         (catch js/Error e
           (rf/dispatch [::e/exception e])
           (is (= (:content (first @notifications))
                  [:div
                   [:h2.font-bold.text-error "Error"]
                   [:div.mt-4 "Error message"]]))))))))
