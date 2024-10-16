(ns element-test
  (:require
   [cljs.test :refer-macros [deftest are is]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]
   [renderer.document.events :as document.e]
   [renderer.element.db :as db]
   [renderer.element.events :as e]
   [renderer.element.subs :as s]))

(deftest init
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [root (rf/subscribe [::s/root])
         root-children (rf/subscribe [::s/root-children])]
     (is (db/valid? @root))
     (are [v sub] (= v sub)
       :canvas (:tag @root)
       :svg (-> @root-children first :tag)))))

(deftest select
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [selected (rf/subscribe [::s/selected])]
     (is (empty? @selected))

     (rf/dispatch [::e/select-all])
     (is (not-empty @selected))

     (rf/dispatch [::e/deselect-all])
     (is (empty? @selected))

     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x 0 :y 0 :width 100 :height 100}}])
     (is :rect (-> @selected first :tag))

     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x 0 :y 0 :width 100 :height 100}}])
     (rf/dispatch [::e/select-same-tags])
     (is (= (count @selected) 2)))))
