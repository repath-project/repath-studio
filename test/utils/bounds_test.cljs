(ns utils.bounds-test
  (:require
   [cljs.test :refer-macros [deftest testing are is]]
   [renderer.i18n.views :refer [t]]
   [renderer.utils.bounds :as utils.bounds]))

(deftest test-union
  (testing "united bounds"
    (are [x y] (= x y)
      [0 0 20 20] (utils.bounds/union [0 0 10 10] [11 11 20 20])
      [0 0 20 20] (utils.bounds/union [0 0 10 10] [9 9 20 20])
      [0 0 11 11] (utils.bounds/union [11 11 11 11] [0 0 0 0]))))

(deftest test-intersect?
  (testing "bounds are intesrecting"
    (is (false? (utils.bounds/intersect? [0 0 10 10] [11 11 20 20])))
    (is (true? (utils.bounds/intersect? [0 0 10 10] [9 9 20 20])))
    (is (true? (utils.bounds/intersect? [0 0 10 10] [10 10 11 11])))))

(deftest test-contained?
  (testing "bounds are contained"
    (is (false? (utils.bounds/contained? [0 0 10 10] [0 0 10 10])))
    (is (true? (utils.bounds/contained? [5 5 10 10] [0 0 20 20])))
    (is (false? (utils.bounds/contained? [0 0 10 10] [1 1 10 10])))))

(deftest test-contained-point?
  (testing "bounds contain point"
    (is (true? (utils.bounds/contained-point? [0 0 10 10] [0 0])))
    (is (true? (utils.bounds/contained-point? [0 0 10 10] [5 5])))
    (is (true? (utils.bounds/contained-point? [0 0 10 10] [10 10])))
    (is (false? (utils.bounds/contained-point? [0 0 10 10] [-5 5])))
    (is (false? (utils.bounds/contained-point? [0 0 10 10] [5 -5])))))

(deftest test-center
  (testing "center of bounds"
    (is (= (utils.bounds/center [0 0 10 10]) [5 5]))))

(deftest test-->snapping-points
  (with-redefs [t (fn [& _] "translation")]
    (testing "snapping points of bounds"
      (is (= (utils.bounds/->snapping-points [0 0 10 10]
                                             #{:corners :centers :midpoints})
             [[0 0] [0 10] [10 0] [10 10] [5 5] [0 5] [10 5] [5 0] [5 10]]))

      (is (= (utils.bounds/->snapping-points [0 0 10 10] #{}) [])))))
