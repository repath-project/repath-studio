(ns repath.bounds-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [repath.studio.bounds :as bounds]))

(deftest test-intersect?
  (testing "check if bounds are intesrecting"
    (is (= (bounds/intersect? [0 0 10 10] [11 11 20 20]) false))
    (is (= (bounds/intersect? [0 0 10 10] [9 9 20 20]) true))
    (is (= (bounds/intersect? [0 0 10 10] [10 10 11 11]) true))))

(deftest test-contained?
  (testing "check if bounds are contained"
    (is (= (bounds/contained? [0 0 10 10] [0 0 10 10]) false))
    (is (= (bounds/contained? [5 5 10 10] [0 0 20 20]) true))
    (is (= (bounds/contained? [0 0 10 10] [1 1 10 10]) false))))