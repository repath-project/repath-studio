(ns document-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf-test]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]
   [renderer.document.db :as db]
   [renderer.document.events :as e]
   [renderer.document.subs :as s]))

(deftest init
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (is (not @(rf/subscribe [::s/some-documents])))
   (is (not @(rf/subscribe [::s/active])))

   (rf/dispatch [::e/init])
   (is @(rf/subscribe [::s/some-documents]))
   (is (db/valid? @(rf/subscribe [::s/active])))
   (is (= "• Untitled-1 - Repath Studio" @(rf/subscribe [::s/title-bar])))))

(deftest close
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (testing "close"
     (rf/dispatch [::e/close (:id @(rf/subscribe [::s/active]) false)])
     (is (not @(rf/subscribe [::s/active]))))

   (testing "close active"
     (rf/dispatch [::e/new])
     (rf/dispatch [::e/saved @(rf/subscribe [::s/active])])
     (rf/dispatch [::e/close-active])
     (is (not @(rf/subscribe [::s/active]))))

   (testing "close all saved"
     (rf/dispatch [::e/new])
     (rf/dispatch [::e/new])
     (rf/dispatch [::e/saved @(rf/subscribe [::s/active])])
     (rf/dispatch [::e/close-all-saved])
     (is (= (count @(rf/subscribe [::s/entities])) 1)))

   (testing "close all"
     (rf/dispatch [::e/saved @(rf/subscribe [::s/active])])
     (rf/dispatch [::e/close-all])
     (is (not @(rf/subscribe [::s/active]))))))

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
     (testing "default color values"
       (is (= @fill "white"))
       (is (= @stroke "black")))

     (testing "swap colors"
       (rf/dispatch [::e/swap-colors])
       (is (= @fill "black"))
       (is (= @stroke "white")))

     (testing "set fill"
       (rf/dispatch [::e/set-attr :fill "red"])
       (is (= @fill "red")))

     (testing "set stroke"
       (rf/dispatch [::e/set-attr :stroke "yellow"])
       (is (= @stroke "yellow"))))))

(deftest filters
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (let [active-filter (rf/subscribe [::s/filter])]
     (testing "default state"
       (is (not @active-filter)))

     (testing "enable filter"
       (rf/dispatch [::e/toggle-filter :blur])
       (is (= @active-filter :blur)))

     (testing "change active filter"
       (rf/dispatch [::e/toggle-filter :deuteranopia])
       (is (= @active-filter :deuteranopia)))

     (testing "disable filter"
       (rf/dispatch [::e/toggle-filter :deuteranopia])
       (is (not @active-filter))))))


(deftest collapse-expand
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (let [collapsed-ids (rf/subscribe [::s/collapsed-ids])
         id (random-uuid)]
     (testing "default state"
       (is (empty? @collapsed-ids)))

     (testing "collapse"
       (rf/dispatch [::e/collapse-el id])
       (is (= #{id} @collapsed-ids)))

     (testing "expand"
       (rf/dispatch [::e/expand-el id])
       (is (empty? @collapsed-ids))))))

(deftest hover
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (let [hovered-ids (rf/subscribe [::s/hovered-ids])
         id (random-uuid)]
     (testing "default state"
       (is (empty? @hovered-ids)))

     (testing "hover"
       (rf/dispatch [::e/set-hovered-id id])
       (is (= #{id} @hovered-ids)))

     (testing "clear hovered"
       (rf/dispatch [::e/clear-hovered])
       (is (empty? @hovered-ids))))))

(deftest save
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/init])

   (let [saved (rf/subscribe [::s/active-saved])
         document (rf/subscribe [::s/active])
         id (:id @document)]
     (testing "default state"
       (is (not @saved)))

     (testing "save"
       (rf/dispatch [::e/saved @document])
       (is @saved)
       (is @(rf/subscribe [::s/saved id]))))))

(deftest load
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/load {:version "100000.0.0" ; Skips migrations.
                           :path "foo/bar/document.rps"
                           :title "document.rps"
                           :elements {}}])

   (is @(rf/subscribe [::s/active-saved]))
   (is (= "foo/bar/document.rps - Repath Studio" @(rf/subscribe [::s/title-bar])))))

(deftest load-multiple
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::e/load-multiple [{:version "100000.0.0"
                                     :path "foo/bar/document-1.rps"
                                     :title "document-1.rps"
                                     :elements {}}
                                    {:version "100000.0.0"
                                     :path "foo/bar/document-2.rps"
                                     :title "document-2.rps"
                                     :elements {}}]])

   (is (= (:title @(rf/subscribe [::s/active])) "document-2.rps"))
   (is (= @(rf/subscribe [::s/recent]) ["foo/bar/document-2.rps"
                                        "foo/bar/document-1.rps"]))))
