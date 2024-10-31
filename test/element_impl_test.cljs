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

(deftest polygon
  (let [polygon-el {:type :element
                    :tag :polygon
                    :attrs {:points "528 -305 718 -370 941 -208"}}]

    (is (= (hierarchy/translate polygon-el [10 10])
           {:type :element
            :tag :polygon
            :attrs {:points "538 -295 728 -360 951 -198"}}))

    (is (= (hierarchy/scale polygon-el [2 2] [25 25])
           {:type :element
            :tag :polygon
            :attrs {:points "503 -265 883 -395 1329 -71"}}))

    (is (= (hierarchy/scale polygon-el [2 2] [0 0])
           {:type :element
            :tag :polygon
            :attrs {:points "528 -240 908 -370 1354 -46"}}))

    (is (= (hierarchy/bounds polygon-el)
           [528 -370 941 -208]))

    (is (= (hierarchy/path polygon-el)
           "M528 -305 718 -370 941 -208z"))

    (is (= (hierarchy/snapping-points polygon-el)
           [[528 -305] [718 -370] [941 -208]]))))

(deftest polyline
  (let [polyline-el {:type :element
                     :tag :polyline
                     :attrs {:points "528 -305 718 -370 941 -208"}}]

    (is (= (hierarchy/translate polyline-el [10 10])
           {:type :element
            :tag :polyline
            :attrs {:points "538 -295 728 -360 951 -198"}}))

    (is (= (hierarchy/scale polyline-el [2 2] [25 25])
           {:type :element
            :tag :polyline
            :attrs {:points "503 -265 883 -395 1329 -71"}}))

    (is (= (hierarchy/scale polyline-el [2 2] [0 0])
           {:type :element
            :tag :polyline
            :attrs {:points "528 -240 908 -370 1354 -46"}}))

    (is (= (hierarchy/bounds polyline-el)
           [528 -370 941 -208]))

    (is (= (hierarchy/path polyline-el)
           "M528 -305 718 -370 941 -208"))

    (is (= (hierarchy/snapping-points polyline-el)
           [[528 -305] [718 -370] [941 -208]]))))

(deftest path
  (let [path-el {:type :element
                 :tag :path
                 :attrs {:d "M528 -305 718 -371 941 -208 663 -174 664 -261z"}}]

    (is (= (hierarchy/translate path-el [10 10])
           {:type :element
            :tag :path
            :attrs {:d "M538-295L728-361 951-198 673-164 674-251z"}}))

    (is (= (hierarchy/scale path-el [2 2] [25 25])
           {:type :element
            :tag :path
            :attrs {:d "M503-264L883-396 1329-70 773-2 775-176z"}}))

    (is (= (hierarchy/scale path-el [2 2] [0 0])
           {:type :element
            :tag :path
            :attrs {:d "M528-239L908-371 1354-45 798 23 800-151z"}}))

    (is (= (hierarchy/bounds path-el)
           [528 -371 941 -174]))

    (is (thrown? js/Error (hierarchy/path path-el)))))

(deftest svg
  (let [svg-el {:type :element
                :tag :svg
                :attrs {:x "0"
                        :y "0"
                        :width "50"
                        :height "50"}}]

    (is (= (hierarchy/translate svg-el [50 50])
           {:type :element
            :tag :svg
            :attrs {:x "50"
                    :y "50"
                    :width "50"
                    :height "50"}}))

    (is (= (hierarchy/scale svg-el [2 2] [25 25])
           {:type :element
            :tag :svg
            :attrs {:x "-25"
                    :y "-25"
                    :width "100"
                    :height "100"}}))

    (is (= (hierarchy/scale svg-el [2 2] [0 0])
           {:type :element
            :tag :svg
            :attrs {:x "0"
                    :y "0"
                    :width "100"
                    :height "100"}}))

    (is (= (hierarchy/bounds svg-el)
           [0 0 50 50]))

    (is (thrown? js/Error (hierarchy/path svg-el)))))

(deftest text
  (let [text-el {:type :element
                 :tag :text
                 :content "My text"
                 :attrs {:x "0"
                         :y "0"
                         :width "50"
                         :height "50"}}]

    (is (= (hierarchy/translate text-el [50 50])
           {:type :element
            :tag :text
            :content "My text"
            :attrs {:x "50"
                    :y "50"
                    :width "50"
                    :height "50"}}))))
