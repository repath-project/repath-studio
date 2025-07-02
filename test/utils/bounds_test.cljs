(ns utils.bounds-test
  (:require
   [cljs.test :refer-macros [deftest testing are is]]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.i18n :as i18n]))

(deftest test-union
  (testing "united bounds"
    (are [x y] (= x y)
      [0 0 20 20] (utils.bounds/union [0 0 10 10] [11 11 20 20])
      [0 0 20 20] (utils.bounds/union [0 0 10 10] [9 9 20 20])
      [0 0 11 11] (utils.bounds/union [11 11 11 11] [0 0 0 0]))))

(deftest test-intersect?
  (testing "bounds are intesrecting"
    (are [x y] (= x y)
      false (utils.bounds/intersect? [0 0 10 10] [11 11 20 20])
      true (utils.bounds/intersect? [0 0 10 10] [9 9 20 20])
      true (utils.bounds/intersect? [0 0 10 10] [10 10 11 11]))))

(deftest test-contained?
  (testing "bounds are contained"
    (are [x y] (= x y)
      false (utils.bounds/contained? [0 0 10 10] [0 0 10 10])
      true (utils.bounds/contained? [5 5 10 10] [0 0 20 20])
      false (utils.bounds/contained? [0 0 10 10] [1 1 10 10]))))

(deftest test-contained-point?
  (testing "bounds contain point"
    (are [x y] (= x y)
      true (utils.bounds/contained-point? [0 0 10 10] [0 0])
      true (utils.bounds/contained-point? [0 0 10 10] [5 5])
      true (utils.bounds/contained-point? [0 0 10 10] [10 10])
      false (utils.bounds/contained-point? [0 0 10 10] [-5 5])
      false (utils.bounds/contained-point? [0 0 10 10] [5 -5]))))

(deftest test-center
  (testing "center of bounds"
    (is (= (utils.bounds/center [0 0 10 10]) [5 5]))))

(deftest test-->snapping-points
  (with-redefs [i18n/t (fn [& _] "translation")]
    (testing "snapping points of bounds"
      (is (= (utils.bounds/->snapping-points [0 0 10 10] #{:corners :centers :midpoints})
             [[0 0] [0 10] [10 0] [10 10] [5 5] [0 5] [10 5] [5 0] [5 10]]))

      (is (= (utils.bounds/->snapping-points [0 0 10 10] #{}) [])))))
