(ns compatibility-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.compatibility :as compatibility]))

(deftest test-version->table
  (testing "version to table"
    (are [x y] (= x y)
      [0 3 3] (compatibility/version->table "0.3.3-2-4cd3bf6-SNAPSHOT")
      [123 0 3] (compatibility/version->table "123.0.3-32423423"))))

(deftest test-requires-migration?
  (testing "migration requirement"
    (are [x y] (= x y)
      true (compatibility/requires-migration? {:version "0.3.3-2-4cd3bf6-SNAPSHOT"} [0 4 0])
      true (compatibility/requires-migration? {:version "0.3.3-2-4cd3bf6-SNAPSHOT"} [0 3 4])
      false (compatibility/requires-migration? {:version "1.3.3-2-4cd3bf6-SNAPSHOT"} [0 3 4])
      false (compatibility/requires-migration? {:version "1.3.3-2-4cd3bf6-SNAPSHOT"} [1 3 3]))))
