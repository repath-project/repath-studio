(ns document-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]
   [renderer.app.subs :as app.subs]
   [renderer.document.db :as db]
   [renderer.document.events :as e]
   [renderer.document.subs :as s]))

(deftest init
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (is (db/valid? @(rf/subscribe [::s/active])))
   (is (= "• Untitled-1 - Repath Studio" @(rf/subscribe [::s/title-bar])))))

(deftest close
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (rf/dispatch [::e/close (:id @(rf/subscribe [::s/active]) false)])
   (is (not @(rf/subscribe [::s/active])))

   (rf/dispatch [::e/new])
   (rf/dispatch [::e/saved @(rf/subscribe [::s/active])])
   (rf/dispatch [::e/close-active])
   (is (not @(rf/subscribe [::s/active])))

   (rf/dispatch [::e/new])
   (rf/dispatch [::e/new])
   (rf/dispatch [::e/saved @(rf/subscribe [::s/active])])
   (rf/dispatch [::e/close-all-saved])
   (is (= (count @(rf/subscribe [::app.subs/documents])) 1))

   (rf/dispatch [::e/saved @(rf/subscribe [::s/active])])
   (rf/dispatch [::e/close-all])
   (is (not @(rf/subscribe [::s/active])))))

(deftest create
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (rf/dispatch [::e/new])
   (is (= "• Untitled-2 - Repath Studio" @(rf/subscribe [::s/title-bar])))

   (rf/dispatch [::e/new-from-template [800 600]])
   (is (= "• Untitled-3 - Repath Studio" @(rf/subscribe [::s/title-bar])))
   (is (= "800" (->>  @(rf/subscribe [::s/elements])
                      (vals)
                      (filter #(= (:tag %) :svg))
                      (first)
                      :attrs
                      :width)))))

(deftest colors
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (let [fill (rf/subscribe [::s/fill])
         stroke (rf/subscribe [::s/stroke])]
     (is (= @fill "white"))
     (is (= @stroke "black"))

     (rf/dispatch [::e/swap-colors])
     (is (= @fill "black"))
     (is (= @stroke "white"))

     (rf/dispatch [::e/set-fill "red"])
     (is (= @fill "red")))))

(deftest filters
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (let [active-filter (rf/subscribe [::s/filter])]
     (is (not @active-filter))

     (rf/dispatch [::e/toggle-filter :blur])
     (is (= @active-filter :blur))

     (rf/dispatch [::e/toggle-filter :deuteranopia])
     (is (= @active-filter :deuteranopia))

     (rf/dispatch [::e/toggle-filter :deuteranopia])
     (is (not @active-filter)))))


(deftest collapse-expand
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (let [collapsed-ids (rf/subscribe [::s/collapsed-ids])
         id (random-uuid)]
     (is (empty? @collapsed-ids))

     (rf/dispatch [::e/collapse-el id])
     (is (= #{id} @collapsed-ids))

     (rf/dispatch [::e/expand-el id])
     (is (empty? @collapsed-ids)))))

(deftest hover
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (let [hovered-ids (rf/subscribe [::s/hovered-ids])
         id (random-uuid)]
     (is (empty? @hovered-ids))

     (rf/dispatch [::e/set-hovered-id id])
     (is (= #{id} @hovered-ids))

     (rf/dispatch [::e/clear-hovered])
     (is (empty? @hovered-ids)))))

(deftest save
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (let [saved (rf/subscribe [::s/active-saved])
         document (rf/subscribe [::s/active])
         id (:id @document)]
     (is (not @saved))

     (rf/dispatch [::e/saved @document])
     (is @saved)

     (is @(rf/subscribe [::s/saved id])))))
