(ns compatibility-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.compatibility :as compatibility]))

(deftest test-version->table
  (testing "version to table"
    (are [x y] (= x y)
      {:major 0 :minor 3 :patch 3} (compatibility/version->table "0.3.3-2-4cd3bf6-SNAPSHOT")
      {:major 123 :minor 0 :patch 3} (compatibility/version->table "123.0.3-32423423"))))

(deftest test-compatible?
  (testing "compatibility check"
    (are [x y] (= x y)
      false (compatibility/compatible? "0.3.3-2-4cd3bf6-SNAPSHOT" "123.0.3-32423423")
      true  (compatibility/compatible? "0.3.3-2-4cd3bf6-SNAPSHOT" "0.3.5-4-7bd6bf6")
      false (compatibility/compatible? nil "123.0.3-32423423")
      true  (compatibility/compatible? nil nil))))
