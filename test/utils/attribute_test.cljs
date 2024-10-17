(ns utils.attribute-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.attribute :as attr]))

(deftest test-str->seq
  (testing "string to sequence conversion"
    (are [x y] (= x y)
      ["0" "1"] (attr/str->seq "0 1")
      ["0" "12" "342.3" "4352345345" "454535" "34"] (attr/str->seq "0 12 342.3 4352345345 454535 34"))))

(deftest test-points->vec
  (testing "string to point vector conversion"
    (are [x y] (= x y)
      [["0" "12"] ["342.3" "4352345345"] ["454535" "34"]] (attr/points->vec "0 12 342.3 4352345345 454535 34")
      [["0" "12"] ["342.3" "4352345345"] ["454535" "34"] ["1"]] (attr/points->vec "0 12 342.3 4352345345 454535 34 1"))))

(deftest test->camel-case
  (testing "attribute key to camel-case conversion"
    (are [x y] (= x y)
      :viewBox (attr/->camel-case :viewbox)
      :glyphOrientationHorizontal (attr/->camel-case :Glyphorientationhorizontal))))

(deftest test->camel-case-memo
  (testing "memoized attribute key to camel-case conversion"
    (are [x y] (= x y)
      :viewBox (attr/->camel-case :viewbox)
      :glyphOrientationHorizontal (attr/->camel-case :Glyphorientationhorizontal))))


(deftest test-defaults
  (testing "default tag attributes"
    (are [x y] (= x y)
      {:x ""
       :y ""
       :rx ""
       :ry ""
       :width ""
       :height ""
       :fill ""
       :stroke ""
       :stroke-dasharray ""
       :stroke-linejoin ""
       :style ""
       :stroke-width ""
       :opacity ""
       :id ""
       :class ""} (attr/defaults :rect))))
