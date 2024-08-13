(ns element-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.tool.core]
   [renderer.utils.element :as element]))

(deftest test-root?
  (testing "is root element"
    (are [x y] (= x y)
      true (element/root? {:type :element :tag :canvas})
      false (element/root? {:type :element :tag :rect}))))
