(ns app-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.i18n.events :as-alias i18n.events]
   [renderer.i18n.subs :as-alias i18n.subs]
   [renderer.panel.events :as-alias panel.events]
   [renderer.panel.subs :as-alias panel.subs]))

(deftest app
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize])

   (testing "language"
     (let [lang (rf/subscribe [::i18n.subs/user-lang])
           system-lang (rf/subscribe [::i18n.subs/system-lang])
           computed-lang (rf/subscribe [::i18n.subs/lang])]
       (testing "default"
         (is (= "system" @lang))
         (is (= "en-US" @system-lang))
         (is (= "en-US" @computed-lang)))

       (testing "set valid language"
         (rf/dispatch [::i18n.events/set-user-lang "el-GR"])
         (is (= "el-GR" @lang))
         (is (= "el-GR" @computed-lang)))))

   (testing "toggling grid"
     (let [grid-visible (rf/subscribe [::app.subs/grid])]
       (is (not @grid-visible))

       (rf/dispatch [::app.events/toggle-grid])
       (is @grid-visible)))

   (testing "toggling panel"
     (let [tree-visible (rf/subscribe [::panel.subs/visible? :tree])]
       (is @tree-visible)

       (rf/dispatch [::panel.events/toggle :tree])
       (is (not @tree-visible))))))

(deftest fonts
  (rf.test/run-test-async
   (rf/dispatch-sync [::app.events/initialize])

   (rf.test/wait-for
    [::app.events/set-loading false]

    (testing "loading system fonts"
      (let [system-fonts (rf/subscribe [::app.subs/system-fonts])
            font-list (rf/subscribe [::app.subs/font-list])]
        (is (not @system-fonts))
        (is (not @font-list))

        (rf/dispatch [::app.events/load-system-fonts])

        (rf.test/wait-for
         [::app.events/set-system-fonts]

         (is (= @font-list ["Adwaita Mono" "Noto Sans"]))))))))
