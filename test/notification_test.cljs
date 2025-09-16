(ns notification-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [fixtures :as fixtures]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.notification.events :as-alias notification.events]
   [renderer.notification.subs :as-alias notification.subs]))

(deftest notification
  (rf.test/run-test-sync
   (fixtures/test-fixtures)
   (rf/dispatch [::app.events/initialize])

   (let [notifications (rf/subscribe [::notification.subs/entities])]

     (testing "initial"
       (is (= @notifications [])))

     (testing "add"
       (rf/dispatch [::notification.events/add [:div "test"]])
       (is (= (count @notifications) 1))
       (is (= (first @notifications) {:content [:div "test"]
                                      :count 1})))

     (testing "merge identical"
       (rf/dispatch [::notification.events/add [:div "test"]])
       (is (= (count @notifications) 1))
       (is (= (first @notifications) {:content [:div "test"]
                                      :count 2})))

     (testing "add different"
       (rf/dispatch [::notification.events/add [:div "test 2"]])
       (is (= (count @notifications) 2))
       (is (= (second @notifications) {:content [:div "test 2"]
                                       :count 1})))

     (testing "remove nth"
       (rf/dispatch [::notification.events/remove-nth 1])
       (is (= (count @notifications) 1)))

     (testing "clear all"
       (rf/dispatch [::notification.events/clear-all])
       (is (= (count @notifications) 0)))

     (testing "string exception"
       (try
         (throw (js/Error. "Error message"))
         (catch js/Error err
           (rf/dispatch [::notification.events/show-exception err])
           (is (= (:content (first @notifications))
                  [:div
                   [:h2.font-bold.text-error "Error"]
                   [:div.mt-4 "Error message"]]))))))))
