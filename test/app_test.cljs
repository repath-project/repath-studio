(ns app-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias e]
   [renderer.app.subs :as-alias s]))

(deftest lang
  (rf-test/run-test-sync
   (rf/dispatch [::e/initialize-db])
   (rf/dispatch [::e/set-lang :en-US])
   (is (= :en-US @(rf/subscribe [::s/lang])))))

(deftest grid
  (rf-test/run-test-sync
   (rf/dispatch [::e/initialize-db])

   (let [grid-visible (rf/subscribe [::s/grid])]
     (is (not @grid-visible))

     (rf/dispatch [::e/toggle-grid])
     (is @grid-visible))))

(deftest panel
  (rf-test/run-test-sync
   (rf/dispatch [::e/initialize-db])

   (let [tree-visible (rf/subscribe [::s/panel-visible? :tree])]
     (is @tree-visible)

     (rf/dispatch [::e/toggle-panel :tree])
     (is (not @tree-visible)))))
