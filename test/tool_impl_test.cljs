(ns tool-impl-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.document.events :as-alias document.e]
   [renderer.element.events :as-alias element.e]
   [renderer.tool.events :as-alias e]
   [renderer.tool.hierarchy :as hierarchy]))

(deftest edit
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (rf/dispatch [::e/activate :edit])

   (is (= (hierarchy/render :edit) [:g ()]))

   (rf/dispatch [::element.e/add {:tag :rect
                                  :attrs {:width 100
                                          :height 100}}])
   (rf/dispatch [::e/activate :edit])

   (is (not= (hierarchy/render :edit) [:g ()]))))
