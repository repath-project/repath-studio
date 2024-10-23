(ns theme-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]
   [renderer.theme.events :as e]
   [renderer.theme.subs :as s]))

(deftest mode
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/set-native-mode :light])

   (let [theme-mode (rf/subscribe [::s/mode])]
     (testing "default theme"
       (is (= :dark @theme-mode)))

     (testing "theme cycling"
       (rf/dispatch [::e/cycle-mode])
       (is (= :light @theme-mode))

       (rf/dispatch [::e/cycle-mode])
       (is (= :system @theme-mode))

       (rf/dispatch [::e/cycle-mode])
       (is (= :dark @theme-mode))))))
