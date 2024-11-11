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
                              :attrs {:width "100"
                                      :height "100"}}])
       (is (= :rect (-> @selected first :tag)))

       (rf/dispatch [::e/add {:tag :rect
                              :attrs {:width "100"
                                      :height "100"}}])
       (rf/dispatch [::e/select-same-tags])
       (is (= (count @selected) 2))))))

(deftest index
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/new-from-template nil])

   (rf/dispatch [::e/add {:tag :rect
                          :attrs {:width "100"
                                  :height "100"}}])
   (rf/dispatch [::e/add {:tag :circle
                          :attrs {:r "100"
                                  :cx "100"
                                  :cy "100"}}])

   (rf/dispatch [::e/add {:tag :line
                          :attrs {:x1 "0"
                                  :y1 "0"
                                  :x2 "50"
                                  :y2 "50"}}])

   (let [elements (rf/subscribe [::s/root-children])]
     (testing "initial order"
       (is (= (mapv :tag @elements) [:rect :circle :line])))

     (testing "lower"
       (rf/dispatch [::e/lower])
       (is (= (mapv :tag @elements) [:rect :line :circle])))

     (testing "raise"
       (rf/dispatch [::e/raise])
       (is (= (mapv :tag @elements) [:rect :circle :line])))

     (testing "raise when already on top"
       (rf/dispatch [::e/raise])
       (is (= (mapv :tag @elements) [:rect :circle :line])))

     (testing "lower to bottom"
       (rf/dispatch [::e/lower-to-bottom])
       (is (= (mapv :tag @elements) [:line :rect :circle])))

     (testing "raise to top"
       (rf/dispatch [::e/raise-to-top])
       (is (= (mapv :tag @elements) [:rect :circle :line]))))))

(deftest align
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/new-from-template [800 600]])

   (rf/dispatch [::e/add {:tag :rect
                          :attrs {:x "50"
                                  :y "50"
                                  :width "100"
                                  :height "100"}}])

   (let [elements (rf/subscribe [::s/selected])]
     (testing "align left"
       (rf/dispatch [::e/align :left])
       (is (= (-> @elements first :attrs) {:x "0"
                                           :y "50"
                                           :width "100"
                                           :height "100"})))

     (testing "align top"
       (rf/dispatch [::e/align :top])
       (is (= (-> @elements first :attrs) {:x "0"
                                           :y "0"
                                           :width "100"
                                           :height "100"})))

     (testing "align right"
       (rf/dispatch [::e/align :right])
       (is (= (-> @elements first :attrs) {:x "700"
                                           :y "0"
                                           :width "100"
                                           :height "100"})))

     (testing "align bottom"
       (rf/dispatch [::e/align :bottom])
       (is (= (-> @elements first :attrs) {:x "700"
                                           :y "500"
                                           :width "100"
                                           :height "100"})))

     (testing "align center vertical"
       (rf/dispatch [::e/align :center-vertical])
       (is (= (-> @elements first :attrs) {:x "700"
                                           :y "250"
                                           :width "100"
                                           :height "100"})))

     (testing "align center horizontal"
       (rf/dispatch [::e/align :center-horizontal])
       (is (= (-> @elements first :attrs) {:x "350"
                                           :y "250"
                                           :width "100"
                                           :height "100"}))))))

(deftest visibility
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:width "100"
                                    :height "100"}}])
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
                            :attrs {:width "100"
                                    :height "100"}}])
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
                            :attrs {:width "100"
                                    :height "100"}}])
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
                            :attrs {:width "100"
                                    :height "100"
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
                            :attrs {:width "100"
                                    :height "100"}}])

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
                            :attrs {:width "100"
                                    :height "100"}}])
     (rf/dispatch [::e/scale [2 4]])
     (is (= (-> @selected first :attrs :width) "200"))
     (is (= (-> @selected first :attrs :height) "400")))))

(deftest translate
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x "100"
                                    :y "100"
                                    :width "100"
                                    :height "100"}}])
     (rf/dispatch [::e/translate [50 100]])
     (is (= (-> @selected first :attrs :x) "150"))
     (is (= (-> @selected first :attrs :y) "200")))))

(deftest place
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x "100"
                                    :y "100"
                                    :width "100"
                                    :height "100"}}])
     (rf/dispatch [::e/place [100 100]])
     (is (= (-> @selected first :attrs :x) "50"))
     (is (= (-> @selected first :attrs :y) "50")))))

(deftest ->path
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])
   (let [selected (rf/subscribe [::s/selected])]
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x "100"
                                    :y "100"
                                    :width "100"
                                    :height "100"
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
                            :attrs {:x "100"
                                    :y "100"
                                    :width "100"
                                    :height "100"
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
                            :attrs {:x "100"
                                    :y "100"
                                    :width "100"
                                    :height "100"
                                    :fill "red"
                                    :stroke "black"}}])
     (rf/dispatch [::e/add {:tag :rect
                            :attrs {:x "100"
                                    :y "100"
                                    :width "100"
                                    :height "100"}}])
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
                          :attrs {:x "100"
                                  :y "100"
                                  :width "100"
                                  :height "100"}}])
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
                          :attrs {:x "100"
                                  :y "100"
                                  :width "100"
                                  :height "100"}}])
   (let [selected (rf/subscribe [::s/selected])
         id (-> @selected first :id)]
     (testing "group"
       (rf/dispatch [::e/group])
       (is (= (-> @selected first :tag) :g))
       (is (= (-> @selected first :children) [id])))

     (testing "ungroup"
       (rf/dispatch [::e/ungroup])
       (is (= (-> @selected first :tag) :rect))))))
