(ns app-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [fixtures :as fixtures]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]))

(deftest app
  (rf.test/run-test-sync
   (fixtures/test-fixtures)
   (rf/dispatch [::app.events/initialize])

   (testing "language"
     (let [lang (rf/subscribe [::app.subs/lang])]
       (testing "default"
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

(deftest fonts
  (rf.test/run-test-async
   (fixtures/test-fixtures)
   (rf/dispatch-sync [::app.events/initialize])

   (testing "loading system fonts"
     (let [system-fonts (rf/subscribe [::app.subs/system-fonts])
           font-list (rf/subscribe [::app.subs/font-list])]
       (is (not @system-fonts))
       (is (not @font-list))

       (rf/dispatch [::app.events/load-system-fonts])

       (rf.test/wait-for
        [::app.events/set-system-fonts]

        (is (= @font-list ["Adwaita Mono" "Noto Sans"])))))))
