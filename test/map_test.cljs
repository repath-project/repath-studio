(ns map-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.tool.core]
   [renderer.utils.map :as map]))

(deftest test-merge-common-with
  (testing "merging common keys with"
    (are [x y] (= x y)
      {:foo "13"} (map/merge-common-with str {:foo 1 :test 2} {:foo 3})
      {} (map/merge-common-with str {:test 2} {:foo 3}))))

(deftest test-remove-nils
  (testing "removing nils from maps"
    (are [x y] (= x y)
      {:foo "bar"} (map/remove-nils {:foo "bar" :test nil})
      {:foo "bar" :test false} (map/remove-nils {:foo "bar" :test false}))))
