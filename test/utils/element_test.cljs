(ns utils.element-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.element.impl.core]
   [renderer.utils.element :as utils.element]))

(deftest test-root?
  (testing "is root element"
    (are [x y] (= x y)
      true (utils.element/root? {:type :element :tag :canvas})
      false (utils.element/root? {:type :element :tag :rect}))))
