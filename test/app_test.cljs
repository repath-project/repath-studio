(ns app-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]))

(deftest lang
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])
   (rf/dispatch [::app.events/set-lang :en-US])
   (is (= :en-US @(rf/subscribe [::app.subs/lang])))))

(deftest grid
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])

   (let [grid-visible (rf/subscribe [::app.subs/grid])]
     (is (not @grid-visible))

     (rf/dispatch [::app.events/toggle-grid])
     (is @grid-visible))))

(deftest panel
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])

   (let [tree-visible (rf/subscribe [::app.subs/panel-visible? :tree])]
     (is @tree-visible)

     (rf/dispatch [::app.events/toggle-panel :tree])
     (is (not @tree-visible)))))
