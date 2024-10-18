(ns element-impl-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.element.impl.core]))

(deftest circle
  (let [circle-el {:type :element
                   :tag :circle
                   :attrs {:cx "0"
                           :cy "0"
                           :r "50"}}]

    (is (= (hierarchy/translate circle-el [50 50])
           {:type :element
            :tag :circle
            :attrs {:cx "50"
                    :cy "50"
                    :r "50"}}))

    (is (= (hierarchy/scale circle-el [2 2] [50 50])
           {:type :element
            :tag :circle
            :attrs {:cx "0"
                    :cy "0"
                    :r "100"}}))

    (is (= (hierarchy/scale circle-el [2 2] [0 0])
           {:type :element
            :tag :circle
            :attrs {:cx "50"
                    :cy "50"
                    :r "100"}}))

    (is (= (hierarchy/bounds circle-el)
           [-50 -50 50 50]))

    (is (= (hierarchy/path circle-el)
           "M 50 0 A 50 50 0 0 1 0 50 A 50 50 0 0 1 -50 0 A 50 50 0 0 1 50 0 z"))))

(deftest rect
  (let [rect-el {:type :element
                 :tag :rect
                 :attrs {:x "0"
                         :y "0"
                         :width "50"
                         :height "50"}}]

    (is (= (hierarchy/translate rect-el [50 50])
           {:type :element
            :tag :rect
            :attrs {:x "50"
                    :y "50"
                    :width "50"
                    :height "50"}}))

    (is (= (hierarchy/scale rect-el [2 2] [25 25])
           {:type :element
            :tag :rect
            :attrs {:x "-25"
                    :y "-25"
                    :width "100"
                    :height "100"}}))

    (is (= (hierarchy/scale rect-el [2 2] [0 0])
           {:type :element
            :tag :rect
            :attrs {:x "0"
                    :y "0"
                    :width "100"
                    :height "100"}}))

    (is (= (hierarchy/bounds rect-el)
           [0 0 50 50]))

    (is (= (hierarchy/path rect-el)
           "M 0 0 H 50 V 50 H 0 V 0 z"))))

(deftest ellipse
  (let [ellipse-el {:type :element
                    :tag :ellipse
                    :attrs {:cx "0"
                            :cy "0"
                            :rx "50"
                            :ry "50"}}]

    (is (= (hierarchy/translate ellipse-el [50 50])
           {:type :element
            :tag :ellipse
            :attrs {:cx "50"
                    :cy "50"
                    :rx "50"
                    :ry "50"}}))

    (is (= (hierarchy/scale ellipse-el [2 2] [25 25])
           {:type :element
            :tag :ellipse
            :attrs {:cx "25"
                    :cy "25"
                    :rx "100"
                    :ry "100"}}))

    (is (= (hierarchy/scale ellipse-el [2 2] [0 0])
           {:type :element
            :tag :ellipse
            :attrs {:cx "50"
                    :cy "50"
                    :rx "100"
                    :ry "100"}}))

    (is (= (hierarchy/bounds ellipse-el)
           [-50 -50 50 50]))

    (is (= (hierarchy/path ellipse-el)
           "M 50 0 A 50 50 0 0 1 0 50 A 50 50 0 0 1 -50 0 A 50 50 0 0 1 50 0 z"))))

(deftest line
  (let [line-el {:type :element
                 :tag :line
                 :attrs {:x1 "0"
                         :y1 "0"
                         :x2 "50"
                         :y2 "50"}}]

    (is (= (hierarchy/translate line-el [50 50])
           {:type :element
            :tag :line
            :attrs {:x1 "50"
                    :y1 "50"
                    :x2 "100"
                    :y2 "100"}}))

    (is (= (hierarchy/scale line-el [2 2] [25 25])
           {:type :element
            :tag :line
            :attrs {:x1 "-25"
                    :y1 "-25"
                    :x2 "75"
                    :y2 "75"}}))

    (is (= (hierarchy/scale line-el [2 2] [0 0])
           {:type :element
            :tag :line
            :attrs {:x1 "0"
                    :y1 "0"
                    :x2 "100"
                    :y2 "100"}}))

    (is (= (hierarchy/bounds line-el)
           [0 0 50 50]))

    (is (= (hierarchy/path line-el)
           "M 0 0 L 50 50"))))
