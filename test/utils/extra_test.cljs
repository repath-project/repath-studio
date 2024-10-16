(ns utils.extra-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.extra :refer [partial-right]]))

(deftest test-partial-right
  (testing "partial right"
    (are [x y] (= x y)
      [1 0 2 0 3 0] (reduce (partial-right conj 0) [] [1 2 3])
      "1a2a3a" (reduce (partial-right str "a") "" [1 2 3]))))
