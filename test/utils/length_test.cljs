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

(deftest test-to-fixed
  (testing "round to precision"
    (are [x y] (= x y)
      "1.111" (utils.length/->fixed 1.111111 3)
      "1" (utils.length/->fixed 1 3)
      "1.000" (utils.length/->fixed 1 3 false)
      "1.11" (utils.length/->fixed 1.111 2)
      "1.11" (utils.length/->fixed 1.114 2)
      "1.12" (utils.length/->fixed 1.116 2)
      "1.11" (utils.length/->fixed 1.11100 2)
      "1" (utils.length/->fixed 1 0)
      "0" (utils.length/->fixed 0 2))))
