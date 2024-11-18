(ns utils.length-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.length :as length]))

(deftest test-valid-unit?
  (testing "check if unit is valid"
    (are [x y] (= x y)
      true (length/valid-unit? "px")
      true (length/valid-unit? "em")
      true (length/valid-unit? "rem")
      false (length/valid-unit? "foo")
      false (length/valid-unit? ""))))
