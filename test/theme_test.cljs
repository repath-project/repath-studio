(ns theme-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.theme.effects :as-alias theme.effects]
   [renderer.theme.events :as-alias theme.events]
   [renderer.theme.subs :as-alias theme.subs]))

(defn test-fixtures
  []
  (rf/reg-cofx
   ::theme.effects/native-mode
   (fn [cofx _]
     (assoc cofx :native-mode :light))))

(deftest mode
  (rf.test/run-test-sync
   (test-fixtures)
   (rf/dispatch [::app.events/initialize-db])
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
