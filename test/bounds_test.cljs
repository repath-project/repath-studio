(ns bounds-test
  (:require
   [cljs.test :refer-macros [deftest testing are is]]
   [renderer.utils.bounds :as bounds]))

(deftest test-union
  (testing "united bounds"
    (are [x y] (= x y)
      [0 0 20 20] (bounds/union [0 0 10 10] [11 11 20 20])
      [0 0 20 20] (bounds/union [0 0 10 10] [9 9 20 20])
      [0 0 11 11] (bounds/union [11 11 11 11] [0 0 0 0]))))

(deftest test-intersect?
  (testing "bounds are intesrecting"
    (are [x y] (= x y)
      false (bounds/intersect? [0 0 10 10] [11 11 20 20])
      true (bounds/intersect? [0 0 10 10] [9 9 20 20])
      true (bounds/intersect? [0 0 10 10] [10 10 11 11]))))

(deftest test-contained?
  (testing "bounds are contained"
    (are [x y] (= x y)
      false (bounds/contained? [0 0 10 10] [0 0 10 10])
      true (bounds/contained? [5 5 10 10] [0 0 20 20])
      false (bounds/contained? [0 0 10 10] [1 1 10 10]))))

(deftest test-contain-point?
  (testing "bounds contain point"
    (are [x y] (= x y)
      true (bounds/contain-point? [0 0 10 10] [0 0])
      true (bounds/contain-point? [0 0 10 10] [5 5])
      true (bounds/contain-point? [0 0 10 10] [10 10])
      false (bounds/contain-point? [0 0 10 10] [-5 5])
      false (bounds/contain-point? [0 0 10 10] [5 -5]))))

(deftest test-center
  (testing "center of bounds"
    (is (= (bounds/center [0 0 10 10]) [5 5]))))
