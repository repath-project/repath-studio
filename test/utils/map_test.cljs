(ns utils.map-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.map :as utils.map]))

(deftest test-merge-common-with
  (testing "merging common keys with"
    (are [x y] (= x y)
      {:foo "13"} (utils.map/merge-common-with str {:foo 1
                                                    :test 2} {:foo 3})
      {} (utils.map/merge-common-with str {:test 2} {:foo 3}))))

(deftest test-remove-nils
  (testing "removing nils from maps"
    (are [x y] (= x y)
      {:foo "bar"} (utils.map/remove-nils {:foo "bar"
                                           :test nil})
      {:foo "bar"
       :test false} (utils.map/remove-nils {:foo "bar"
                                            :test false}))))
