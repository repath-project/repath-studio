(ns frame-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]
   [renderer.document.events :as document.e]
   [renderer.document.subs :as document.s]
   [renderer.element.events :as element.e]
   [renderer.frame.events :as e]
   [renderer.frame.subs :as s]))

(deftest frame
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   (testing "resize"
     (let [viewbox (rf/subscribe [::s/viewbox])]
       (rf/dispatch [::e/resize {:x 252
                                 :y 139
                                 :width 1946
                                 :height 945
                                 :top 139
                                 :right 2198
                                 :bottom 1084
                                 :left 252}])

       (is (= @viewbox [0 0 1946 945]))
       (is (= @(rf/subscribe [::s/viewbox-attr]) "0 0 1946 945")))

     (testing "zoom"
       (let [zoom (rf/subscribe [::document.s/zoom])]
         (is (= @zoom 1))

         (rf/dispatch [::e/zoom-in])
         (is (> @zoom 1))

         (rf/dispatch [::e/zoom-out])
         (is (= @zoom 1))

         (rf/dispatch [::e/zoom-out])
         (is (< @zoom 1))

         (rf/dispatch [::e/set-zoom 1])
         (is (= @zoom 1))))

     (testing "focus"
       (let [viewbox (rf/subscribe [::s/viewbox])
             zoom (rf/subscribe [::document.s/zoom])
             pan (rf/subscribe [::document.s/pan])]
         (is (= @zoom 1))

         (rf/dispatch [::element.e/select-all])

         (rf/dispatch [::e/focus-selection :original])
         (is (= @viewbox [-675.5 -51.5 1946 945]))
         (is (= @zoom 1))
         (is (= @pan [-675.5 -51.5]))

         (rf/dispatch [::e/focus-selection :fit])
         (is (> @zoom 1)))))))
