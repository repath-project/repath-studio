(ns theme-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.theme.events :as-alias theme.events]
   [renderer.theme.subs :as-alias theme.subs]))

(deftest mode
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (let [theme-mode (rf/subscribe [::theme.subs/mode])]

     (testing "default theme"
       (is (= :system @theme-mode)))

     (testing "theme cycling"
       (rf/dispatch [::theme.events/cycle-mode])
       (is (= :dark @theme-mode))

       (rf/dispatch [::theme.events/cycle-mode])
       (is (= :light @theme-mode))

       (rf/dispatch [::theme.events/cycle-mode])
       (is (= :system @theme-mode))))))
