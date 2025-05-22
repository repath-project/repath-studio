(ns window-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.window.effects :as-alias window.effects]
   [renderer.window.events :as-alias window.events]
   [renderer.window.subs :as-alias window.subs]))

(defn test-fixtures
  []
  (rf/reg-cofx
   ::window.effects/fullscreen
   (fn [cofx _]
     (assoc cofx :fullscreen true)))

  (rf/reg-cofx
   ::window.effects/focused
   (fn [cofx _]
     (assoc cofx :focused false))))

(deftest window
  (rf.test/run-test-sync
   (test-fixtures)
   (rf/dispatch [::app.events/initialize-db])

   (let [maximized (rf/subscribe [::window.subs/maximized?])
         fullscreen (rf/subscribe [::window.subs/fullscreen?])
         focused (rf/subscribe [::window.subs/focused?])]

     (testing "maximize"
       (rf/dispatch [::window.events/set-maximized false])
       (is (not @maximized))

       (rf/dispatch [::window.events/set-maximized true])
       (is @maximized))

     (testing "fullscreen"
       (rf/dispatch [::window.events/update-fullscreen])
       (is @fullscreen)

       (rf/dispatch [::window.events/set-fullscreen false])
       (is (not @fullscreen)))

     (testing "focused"
       (rf/dispatch [::window.events/update-focused])
       (is (not @focused))

       (rf/dispatch [::window.events/set-focused true])
       (is @focused)))))
