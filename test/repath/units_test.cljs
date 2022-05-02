(ns repath.units-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [repath.studio.units :as units]))

(deftest test-unit->key
  (testing "convert unit string to keyword"
    (is (= (units/unit->key "px") :px))
    (is (= (units/unit->key "Px") :px))))

(deftest test-valid-unit?
  (testing "check if unit is valid"
    (is (= (units/valid-unit? "px") true))
    (is (= (units/valid-unit? "em") true))
    (is (= (units/valid-unit? "rem") true))
    (is (= (units/valid-unit? "foo") false))
    (is (= (units/valid-unit? "") false))))

(deftest test-match-unit
  (testing "match unit"
    (is (= (units/match-unit "5px") "px"))
    ;; TODO The following case should not work. We need to adjust the regex.
    (is (= (units/match-unit "5 px") "px"))
    (is (= (units/match-unit "5454px") "px"))
    (is (= (units/match-unit "px") "px"))
    (is (= (units/match-unit "0") ""))))

(deftest test-parse-unit
  (testing "parse unit"
    (is (= (units/parse-unit "5px") [5 "px"]))
    (is (= (units/parse-unit " 5px ") [5 "px"]))
    (is (= (units/parse-unit " 5 px ") [5 "px"]))
    (is (= (units/parse-unit "5454px") [5454 "px"]))
    (is (= (units/parse-unit "px") [0 "px"]))
    (is (= (units/parse-unit "0") [0 ""]))))
