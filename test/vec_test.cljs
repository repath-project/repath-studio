(ns vec-test
  (:require
   [cljs.test :refer-macros [deftest testing are]]
   [renderer.utils.vec :as vec]))

(deftest test-remove-nth
  (testing "removing bth element of vector"
    (are [x y] (= x y)
      [:a :b :d] (vec/remove-nth [:a :b :c :d] 2)
      [:a :b :c] (vec/remove-nth [:a :b :c :d] 3))))

(deftest test-add
  (testing "adding an element to index"
    (are [x y] (= x y)
      [:a :b :c :d] (vec/add [:a :b :c] 3 :d)
      [:a :x :b :c :d] (vec/add [:a :b :c :d] 1 :x))))

(deftest test-move
  (testing "moving element by index"
    (are [x y] (= x y)
      [:a :b :d :c] (vec/move [:a :b :c :d] 2 3)
      [:a :b :c :d] (vec/move [:a :b :c :d] 1 1))))

(deftest test-swap
  (testing "swapping elements by index"
    (are [x y] (= x y)
      [:d :b :c :a] (vec/swap [:a :b :c :d] 0 3)
      [:a :b :c :d] (vec/swap [:a :b :c :d] 1 1))))
