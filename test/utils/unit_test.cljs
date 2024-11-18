(ns utils.unit-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.unit :as unit]))

(deftest test-->key
  (testing "convert unit string to keyword"
    (are [x y] (= x y)
      :px (unit/->key "px")
      :px (unit/->key "Px"))))

(deftest test-unit
  (testing "match unit"
    (are [x y] (= x y)
      "px" (unit/match "5px")
      ;; TODO: The following case should not work. We need to adjust the regex.
      "px" (unit/match "5 px")
      "px" (unit/match "5454px")
      "px" (unit/match "px")
      "" (unit/match "0"))))

(deftest test-parse-unit
  (testing "parse unit"
    (are [x y] (= x y)
      [5 "px"] (unit/parse "5px")
      [5 "px"] (unit/parse " 5px ")
      [5 "px"] (unit/parse " 5 px ")
      [5454 "px"] (unit/parse "5454px")
      [0 "px"] (unit/parse "px")
      [0 ""] (unit/parse "0"))))
