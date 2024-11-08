(ns element-test
  (:require
   ["paper" :refer [paper]]
   [cljs.test :refer-macros [deftest are is testing]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.document.events :as-alias document.e]
   [renderer.element.db :as db]
   [renderer.element.events :as-alias e]
   [renderer.element.subs :as-alias s]))

(.setup paper)

(deftest init
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [root (rf/subscribe [::s/root])
         root-children (rf/subscribe [::s/root-children])]
     (testing "default elements"
       (is (db/valid? @root))
       (are [v sub] (= v sub)
         :canvas (:tag @root)
         :svg (-> @root-children first :tag))))))

(deftest select
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [selected (rf/subscribe [::s/selected])]
     (testing "default state"
       (is (empty? @selected)))

     (testing "select all"
       (rf/dispatch [::e/select-all])
       (is (= (count @selected) 1)))

     (testing "deselect all"
       (rf/dispatch [::e/deselect-all])
       (is (empty? @selected)))

     (testing "select same tags"
       (rf/dispatch [::e/add {:tag :rect
                              :attrs {:width 100
                                      :height 100}}])
       (is (= :rect (-> @selected first :tag)))

       (rf/dispatch [::e/add {:tag :rect
                              :attrs {:width 100
                                      :height 100}}])
       (rf/dispatch [::e/select-same-tags])
       (is (= (count @selected) 2))))))

(deftest visibility
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100
                                    :height 100}}])
     (testing "default state"
       (is (-> @selected first :visible)))

     (testing "toggle visibility"
       (rf/dispatch [::e/toggle-prop (-> @selected first :id) :visible])
       (is (not (-> @selected first :visible))))

     (testing "revert visibility"
       (rf/dispatch [::e/toggle-prop (-> @selected first :id) :visible])
       (is (-> @selected first :visible))))))

(deftest label
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100
                                    :height 100}}])
     (testing "default state"
       (is (not (-> @selected first :label))))

     (testing "set label"
       (rf/dispatch [::e/set-prop (-> @selected first :id) :label "rect"])
       (is (= (-> @selected first :label) "rect")))

     (testing "clear label"
       (rf/dispatch [::e/set-prop (-> @selected first :id) :label ""])
       (is (not (-> @selected first :label)))))))

(deftest lock
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100
                                    :height 100}}])
     (testing "default state"
       (is (not (-> @selected first :locked))))

     (testing "lock"
       (rf/dispatch [::e/lock])
       (is (-> @selected first :locked)))

     (testing "unlock"
       (rf/dispatch [::e/unlock])
       (is (not (-> @selected first :locked)))))))

(deftest attribute
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100
                                    :height 100
                                    :fill "white"}}])
     (is (= (-> @selected first :attrs :fill) "white"))

     (testing "set attribute"
       (rf/dispatch [::e/set-attr :fill "red"])
       (is (= (-> @selected first :attrs :fill) "red")))

     (testing "preview attribute"
       (rf/dispatch [::e/preview-attr :fill "yellow"])
       (is (= (-> @selected first :attrs :fill) "yellow")))

     (testing "remove attribute"
       (rf/dispatch [::e/remove-attr :fill])
       (is (not (-> @selected first :attrs :fill)))))))

(deftest delete
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100
                                    :height 100}}])

     (testing "delete selection"
       (let [id (-> @selected first :id)]
         (is (uuid? id))
         (rf/dispatch [::e/delete])
         (is (empty? @selected))
         (is (not @(rf/subscribe [::s/entity id]))))))))

(deftest scale
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width 100
                                    :height 100}}])
     (rf/dispatch [::e/scale [2 4]])
     (is (= (-> @selected first :attrs :width) "200"))
     (is (= (-> @selected first :attrs :height) "400")))))

(deftest translate
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x 100
                                    :y 100
                                    :width 100
                                    :height 100}}])
     (rf/dispatch [::e/translate [50 100]])
     (is (= (-> @selected first :attrs :x) "150"))
     (is (= (-> @selected first :attrs :y) "200")))))

(deftest place
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x 100
                                    :y 100
                                    :width 100
                                    :height 100}}])
     (rf/dispatch [::e/place [100 100]])
     (is (= (-> @selected first :attrs :x) "50"))
     (is (= (-> @selected first :attrs :y) "50")))))

(deftest ->path
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x 100
                                    :y 100
                                    :width 100
                                    :height 100
                                    :fill "red"}}])
     (rf/dispatch [::e/->path])
     (is (= (-> @selected first :tag) :path))
     (is (= (-> @selected first :attrs :fill) "red"))
     (not (-> @selected first :attrs :x)))))

(deftest stroke->path
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x 100
                                    :y 100
                                    :width 100
                                    :height 100
                                    :fill "red"
                                    :stroke "black"}}])
     (rf/dispatch [::e/stroke->path])
     (is (= (-> @selected first :tag) :path))
     (is (= (-> @selected first :attrs :fill) "black"))
     (not (-> @selected first :attrs :stroke)))))

(deftest boolean-operation
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x 100
                                    :y 100
                                    :width 100
                                    :height 100
                                    :fill "red"
                                    :stroke "black"}}])
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x 100
                                    :y 100
                                    :width 100
                                    :height 100}}])
     (rf/dispatch [::e/select-all])
     (rf/dispatch [::e/boolean-operation :unite])
     (is (= (-> @selected first :tag) :path))
     (is (= (-> @selected first :attrs :fill) "red")))))

(deftest import-svg
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/import {:svg "<svg x=\"100\" y=\"100\" width=\"200\" height=\"200\"></svg>"
                               :label "filename.svg"
                               :position [500 500]}])
     (is (= (-> @selected first :tag) :svg))
     (is (= (-> @selected first :label) "filename.svg"))
     (is (= (-> @selected first :attrs :x) "500"))
     (is (= (-> @selected first :attrs :width) "200")))))

(deftest animate
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (rf/dispatch [::e/add {:tag :rect
                          :attrs {:x 100
                                  :y 100
                                  :width 100
                                  :height 100}}])
   (let [selected (rf/subscribe [::s/selected])
         id (-> @selected first :id)]
     (rf/dispatch [::e/animate :animate {}])
     (is (= (-> @selected first :tag) :animate))
     (is (= (-> @selected first :parent) id)))))

(deftest group
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (rf/dispatch [::e/add {:tag :rect
                          :attrs {:x 100
                                  :y 100
                                  :width 100
                                  :height 100}}])
   (let [selected (rf/subscribe [::s/selected])
         id (-> @selected first :id)]
     (testing "group"
       (rf/dispatch [::e/group])
       (is (= (-> @selected first :tag) :g))
       (is (= (-> @selected first :children) [id])))

     (testing "ungroup"
       (rf/dispatch [::e/ungroup])
       (is (= (-> @selected first :tag) :rect))))))
