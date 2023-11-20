(ns bounds-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [renderer.utils.bounds :as bounds]))

(deftest test-intersect-bounds?
  (testing "check if bounds are intesrecting"
    (is (= (bounds/intersect-bounds? [0 0 10 10] [11 11 20 20]) false))
    (is (= (bounds/intersect-bounds? [0 0 10 10] [9 9 20 20]) true))
    (is (= (bounds/intersect-bounds? [0 0 10 10] [10 10 11 11]) true))))

(deftest test-contain-bounds?
  (testing "check if bounds are contained"
    (is (= (bounds/contain-bounds? [0 0 10 10] [0 0 10 10]) false))
    (is (= (bounds/contain-bounds? [5 5 10 10] [0 0 20 20]) true))
    (is (= (bounds/contain-bounds? [0 0 10 10] [1 1 10 10]) false))))

(deftest test-contain-point?
  (testing "check if bounds contain point"
    (is (= (bounds/contain-point? [0 0 10 10] [0 0]) true))
    (is (= (bounds/contain-point? [0 0 10 10] [5 5]) true))
    (is (= (bounds/contain-point? [0 0 10 10] [10 10]) true))
    (is (= (bounds/contain-point? [0 0 10 10] [-5 5]) false))
    (is (= (bounds/contain-point? [0 0 10 10] [5 -5]) false))))