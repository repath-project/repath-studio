(ns utils.length-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.length :as utils.length]))

(deftest test-valid-unit?
  (testing "check if unit is valid"
    (are [x y] (= x y)
      true (utils.length/valid-unit? "px")
      true (utils.length/valid-unit? "em")
      true (utils.length/valid-unit? "rem")
      false (utils.length/valid-unit? "foo")
      false (utils.length/valid-unit? ""))))
