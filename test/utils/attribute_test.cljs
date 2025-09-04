(ns utils.attribute-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.i18n :as i18n]))

(deftest test-str->seq
  (testing "string to sequence conversion"
    (are [x y] (= x y)
      ["0" "1"]
      (utils.attribute/str->seq "0 1")

      ["0" "12" "342.3" "4352345345" "454535" "34"]
      (utils.attribute/str->seq "0 12 342.3 4352345345 454535 34"))))

(deftest test-points->vec
  (testing "string to point vector conversion"
    (are [x y] (= x y)
      [["0" "12"] ["342.3" "4352345345"] ["454535" "34"]]
      (utils.attribute/points->vec "0 12 342.3 4352345345 454535 34")

      [["0" "12"] ["342.3" "4352345345"] ["454535" "34"] ["1"]]
      (utils.attribute/points->vec "0 12 342.3 4352345345 454535 34 1"))))

(deftest test->camel-case
  (testing "attribute key to camel-case conversion"
    (are [x y] (= x y)
      :viewBox (utils.attribute/->camel-case :viewbox)
      :glyphOrientationHorizontal (utils.attribute/->camel-case
                                   :Glyphorientationhorizontal))))

(deftest test->camel-case-memo
  (testing "memoized attribute key to camel-case conversion"
    (are [x y] (= x y)
      :viewBox (utils.attribute/->camel-case :viewbox)
      :glyphOrientationHorizontal (utils.attribute/->camel-case
                                   :Glyphorientationhorizontal))))

(deftest test-defaults
  (with-redefs [i18n/t (fn [& _] "translation")]
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
         :class ""} (utils.attribute/defaults :rect)))))
