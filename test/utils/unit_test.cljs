(ns utils.unit-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.unit :as utils.unit]))

(deftest test-->key
  (testing "convert unit string to keyword"
    (are [x y] (= x y)
      :px (utils.unit/->key "px")
      :px (utils.unit/->key "Px"))))

(deftest test-unit
  (testing "match unit"
    (are [x y] (= x y)
      "px" (utils.unit/match "5px")
      ;; TODO: The following case should not work. We need to adjust the regex.
      "px" (utils.unit/match "5 px")
      "px" (utils.unit/match "5454px")
      "px" (utils.unit/match "px")
      "" (utils.unit/match "0"))))

(deftest test-parse-unit
  (testing "parse unit"
    (are [x y] (= x y)
      [5 "px"] (utils.unit/parse "5px")
      [5 "px"] (utils.unit/parse " 5px ")
      [5 "px"] (utils.unit/parse " 5 px ")
      [5454 "px"] (utils.unit/parse "5454px")
      [0 "px"] (utils.unit/parse "px")
      [0 ""] (utils.unit/parse "0"))))
