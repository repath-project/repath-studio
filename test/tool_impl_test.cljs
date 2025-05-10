(ns tool-impl-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.events :as-alias document.events]
   [renderer.element.events :as-alias element.events]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(deftest edit
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])
   (rf/dispatch [::document.events/init])
   (rf/dispatch [::tool.events/activate :edit])

   (is (= (tool.hierarchy/render :edit) [:g ()]))

   (rf/dispatch [::element.events/add {:tag :rect
                                       :attrs {:width 100
                                               :height 100}}])
   (rf/dispatch [::tool.events/activate :edit])

   (is (not= (tool.hierarchy/render :edit) [:g ()]))))
