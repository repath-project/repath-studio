(ns app-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]))

(defn test-fixtures
  []
  (rf/reg-cofx
   ::app.effects/system-language
   (fn [coeffects _]
     (assoc coeffects :system-language "en-US"))))

(deftest app
  (rf.test/run-test-sync
   (test-fixtures)
   (rf/dispatch [::app.events/initialize-db])

   (testing "language"
     (let [lang (rf/subscribe [::app.subs/lang])]
       (testing "default"
         (is (not @lang)))

       (testing "initialization"
         (rf/dispatch [::app.events/init-lang])
         (is (= "en-US" @lang)))

       (testing "set valid language"
         (rf/dispatch [::app.events/set-lang "el-GR"])
         (is (= "el-GR" @lang)))

       (testing "set invalid language"
         (rf/dispatch [::app.events/set-lang "foo-Bar"])
         (is (= "el-GR" @lang)))))

   (testing "toggling grid"
     (let [grid-visible (rf/subscribe [::app.subs/grid])]
       (is (not @grid-visible))

       (rf/dispatch [::app.events/toggle-grid])
       (is @grid-visible)))

   (testing "toggling panel"
     (let [tree-visible (rf/subscribe [::app.subs/panel-visible? :tree])]
       (is @tree-visible)

       (rf/dispatch [::app.events/toggle-panel :tree])
       (is (not @tree-visible))))))
