(ns bounds-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [renderer.utils.bounds :as bounds]))

(deftest test-union
  (testing "united bounds"
    (is (= (bounds/union [0 0 10 10] [11 11 20 20]) [0 0 20 20]))
    (is (= (bounds/union [0 0 10 10] [9 9 20 20]) [0 0 20 20]))
    (is (= (bounds/union [11 11 11 11] [0 0 0 0]) [0 0 11 11]))))


(deftest test-intersected?
  (testing "bounds are intesrecting"
    (is (= (bounds/intersected? [0 0 10 10] [11 11 20 20]) false))
    (is (= (bounds/intersected? [0 0 10 10] [9 9 20 20]) true))
    (is (= (bounds/intersected? [0 0 10 10] [10 10 11 11]) true))))

(deftest test-contained?
  (testing "bounds are contained"
    (is (= (bounds/contained? [0 0 10 10] [0 0 10 10]) false))
    (is (= (bounds/contained? [5 5 10 10] [0 0 20 20]) true))
    (is (= (bounds/contained? [0 0 10 10] [1 1 10 10]) false))))

(deftest test-contained-point?
  (testing "bounds contain point"
    (is (= (bounds/contained-point? [0 0 10 10] [0 0]) true))
    (is (= (bounds/contained-point? [0 0 10 10] [5 5]) true))
    (is (= (bounds/contained-point? [0 0 10 10] [10 10]) true))
    (is (= (bounds/contained-point? [0 0 10 10] [-5 5]) false))
    (is (= (bounds/contained-point? [0 0 10 10] [5 -5]) false))))

(deftest test-center
  (testing "center of bounds"
    (is (= (bounds/center [0 0 10 10]) [5 5]))))
