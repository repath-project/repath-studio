(ns units-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.units :as units]))

(deftest test-unit->key
  (testing "convert unit string to keyword"
    (are [x y] (= x y)
      :px (units/unit->key "px")
      :px (units/unit->key "Px"))))

(deftest test-valid-unit?
  (testing "check if unit is valid"
    (are [x y] (= x y)
      true (units/valid-unit? "px")
      true (units/valid-unit? "em")
      true (units/valid-unit? "rem")
      false (units/valid-unit? "foo")
      false (units/valid-unit? ""))))

(deftest test-match-unit
  (testing "match unit"
    (are [x y] (= x y)
      "px" (units/match-unit "5px")
      ;; TODO: The following case should not work. We need to adjust the regex.
      "px" (units/match-unit "5 px")
      "px" (units/match-unit "5454px")
      "px" (units/match-unit "px")
      "" (units/match-unit "0"))))

(deftest test-parse-unit
  (testing "parse unit"
    (are [x y] (= x y)
      [5 "px"] (units/parse-unit "5px")
      [5 "px"] (units/parse-unit " 5px ")
      [5 "px"] (units/parse-unit " 5 px ")
      [5454 "px"] (units/parse-unit "5454px")
      [0 "px"] (units/parse-unit "px")
      [0 ""] (units/parse-unit "0"))))
