(ns app-test
  (:require
   [cljs.test :refer-macros [deftest is are]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as e]
   [renderer.app.subs :as s]
   [renderer.document.subs :as document.s]))

(deftest init
  (rf-test/run-test-sync
   (rf/dispatch [::e/initialize-db])

   (are [v sub] (= v sub)
     :transform @(rf/subscribe [::s/tool])
     {} @(rf/subscribe [::document.s/entities])
     [] @(rf/subscribe [::document.s/tabs]))))

(deftest tool
  (rf-test/run-test-sync
   (rf/dispatch [::e/initialize-db])

   (let [active-tool (rf/subscribe [::s/tool])]
     (is (= @active-tool :transform))

     (rf/dispatch [::e/set-tool :rect])
     (is (= @active-tool :rect)))))

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

   (let [tree-visible (rf/subscribe [::s/panel-visible :tree])]
     (is @tree-visible)

     (rf/dispatch [::e/toggle-panel :tree])
     (is (not @tree-visible)))))
