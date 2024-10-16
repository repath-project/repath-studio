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
     (is (= (count @selected) 1))

     (rf/dispatch [::e/deselect-all])
     (is (empty? @selected))

     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100 :height 100}}])
     (is (= :rect (-> @selected first :tag)))

     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100 :height 100}}])
     (rf/dispatch [::e/select-same-tags])
     (is (= (count @selected) 2)))))

(deftest lock
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100 :height 100}}])
     (is (not (-> @selected first :locked)))

     (rf/dispatch [::e/lock])
     (is (-> @selected first :locked))

     (rf/dispatch [::e/unlock])
     (is (not (-> @selected first :locked))))))

(deftest attribute
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100 :height 100 :fill "white"}}])
     (is (= (-> @selected first :attrs :fill) "white"))

     (rf/dispatch [::e/set-fill "red"])
     (is (= (-> @selected first :attrs :fill) "red"))

     (rf/dispatch [::e/preview-stroke "yellow"])
     (is (= (-> @selected first :attrs :fill) "yellow"))

     (rf/dispatch [::e/remove-attr :fill])
     (is (not (-> @selected first :attrs :fill))))))

(deftest scale
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100 :height 100}}])
     (rf/dispatch [::e/scale [2 4]])
     (is (= (-> @selected first :attrs :width) "200"))
     (is (= (-> @selected first :attrs :height) "400")))))

(deftest translate
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x 100 :y 100 :width 100 :height 100}}])
     (rf/dispatch [::e/translate [50 100]])
     (is (= (-> @selected first :attrs :x) "150"))
     (is (= (-> @selected first :attrs :y) "200")))))

(deftest place
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x 100 :y 100 :width 100 :height 100}}])
     (rf/dispatch [::e/place [100 100]])
     (is (= (-> @selected first :attrs :x) "50"))
     (is (= (-> @selected first :attrs :y) "50")))))
