(ns theme-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [fixtures :as fixtures]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.theme.events :as-alias theme.events]
   [renderer.theme.subs :as-alias theme.subs]))

(deftest mode
  (rf.test/run-test-sync
   (fixtures/test-fixtures)
   (rf/dispatch [::app.events/initialize])
   (rf/dispatch [::theme.events/set-document-attr])

   (let [theme-mode (rf/subscribe [::theme.subs/mode])]

     (testing "default theme"
       (is (= :dark @theme-mode)))

     (testing "theme cycling"
       (rf/dispatch [::theme.events/cycle-mode])
       (is (= :light @theme-mode))

       (rf/dispatch [::theme.events/cycle-mode])
       (is (= :system @theme-mode))

       (rf/dispatch [::theme.events/cycle-mode])
       (is (= :dark @theme-mode))))))
