(ns document-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rf.test]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.document.db :as document.db]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]))

(deftest document
  (rf.test/run-test-sync
   (rf/dispatch [::app.events/initialize-db])

   (testing "defaults"
     (is (not @(rf/subscribe [::document.subs/entities?])))
     (is (not @(rf/subscribe [::document.subs/active]))))

   (testing "initialization"
     (rf/dispatch [::document.events/init])
     (is @(rf/subscribe [::document.subs/entities?]))
     (is (document.db/valid? @(rf/subscribe [::document.subs/active])))
     (is (= "• Untitled-1 - Repath Studio" @(rf/subscribe [::document.subs/title-bar]))))

   (testing "close"
     (rf/dispatch [::document.events/close @(rf/subscribe [::document.subs/active-id]) false])
     (is (not @(rf/subscribe [::document.subs/active]))))

   (testing "close active"
     (rf/dispatch [::document.events/new])
     (rf/dispatch [::document.events/saved @(rf/subscribe [::document.subs/active])])
     (rf/dispatch [::document.events/close-active])
     (is (not @(rf/subscribe [::document.subs/active]))))

   (testing "close saved"
     (rf/dispatch [::document.events/new])
     (rf/dispatch [::document.events/new])
     (rf/dispatch [::document.events/saved @(rf/subscribe [::document.subs/active])])
     (rf/dispatch [::document.events/close-saved])
     (is (= (count @(rf/subscribe [::document.subs/entities])) 1)))

   (testing "close all"
     (rf/dispatch [::document.events/saved @(rf/subscribe [::document.subs/active])])
     (rf/dispatch [::document.events/close-all])
     (is (not @(rf/subscribe [::document.subs/active]))))

   (testing "create"
     (rf/dispatch [::document.events/new])
     (is (= "• Untitled-1 - Repath Studio" @(rf/subscribe [::document.subs/title-bar])))

     (rf/dispatch [::document.events/new-from-template [800 600]])
     (is (= "• Untitled-2 - Repath Studio" @(rf/subscribe [::document.subs/title-bar])))
     (is (= "800" (->>  @(rf/subscribe [::document.subs/elements])
                        (vals)
                        (filter #(= (:tag %) :svg))
                        (first)
                        :attrs
                        :width))))

   (testing "colors"
     (let [fill (rf/subscribe [::document.subs/fill])
           stroke (rf/subscribe [::document.subs/stroke])]
       (testing "default color values"
         (is (= @fill "white"))
         (is (= @stroke "black")))

       (testing "swap colors"
         (rf/dispatch [::document.events/swap-colors])
         (is (= @fill "black"))
         (is (= @stroke "white")))

       (testing "set fill"
         (rf/dispatch [::document.events/set-attr :fill "red"])
         (is (= @fill "red")))

       (testing "set stroke"
         (rf/dispatch [::document.events/set-attr :stroke "yellow"])
         (is (= @stroke "yellow")))))

   (testing "filters"
     (let [active-filter (rf/subscribe [::document.subs/filter])]
       (testing "default state"
         (is (not @active-filter)))

       (testing "enable filter"
         (rf/dispatch [::document.events/toggle-filter :blur])
         (is (= @active-filter :blur)))

       (testing "change active filter"
         (rf/dispatch [::document.events/toggle-filter :deuteranopia])
         (is (= @active-filter :deuteranopia)))

       (testing "disable filter"
         (rf/dispatch [::document.events/toggle-filter :deuteranopia])
         (is (not @active-filter)))))

   (testing "collapse/expand elements"
     (let [collapsed-ids (rf/subscribe [::document.subs/collapsed-ids])
           id (random-uuid)]
       (testing "default state"
         (is (empty? @collapsed-ids)))

       (testing "collapse"
         (rf/dispatch [::document.events/collapse-el id])
         (is (= #{id} @collapsed-ids)))

       (testing "expand"
         (rf/dispatch [::document.events/expand-el id])
         (is (empty? @collapsed-ids)))))

   (testing "hover elements"
     (let [hovered-ids (rf/subscribe [::document.subs/hovered-ids])
           id (random-uuid)]
       (testing "default state"
         (is (empty? @hovered-ids)))

       (testing "hover"
         (rf/dispatch [::document.events/set-hovered-id id])
         (is (= #{id} @hovered-ids)))

       (testing "clear hovered"
         (rf/dispatch [::document.events/clear-hovered])
         (is (empty? @hovered-ids)))))

   (testing "save"
     (let [saved (rf/subscribe [::document.subs/active-saved?])
           active-document (rf/subscribe [::document.subs/active])
           id (:id @active-document)]
       (testing "default state"
         (is (not @saved)))

       (testing "save"
         (rf/dispatch [::document.events/saved @active-document])
         (is @saved)
         (is @(rf/subscribe [::document.subs/saved? id])))))

   (testing "load"
     (rf/dispatch [::document.events/load {:version "100000.0.0" ; Skips migrations.
                                           :path "foo/bar/document.rps"
                                           :title "document.rps"
                                           :elements {}}])

     (is @(rf/subscribe [::document.subs/active-saved?]))
     (is (= "foo/bar/document.rps - Repath Studio" @(rf/subscribe [::document.subs/title-bar]))))

   (testing "load multiple"
     (rf/dispatch [::document.events/load-multiple [{:version "100000.0.0"
                                                     :path "foo/bar/document-1.rps"
                                                     :title "document-1.rps"
                                                     :elements {}}
                                                    {:version "100000.0.0"
                                                     :path "foo/bar/document-2.rps"
                                                     :title "document-2.rps"
                                                     :elements {}}]])

     (is (= (:title @(rf/subscribe [::document.subs/active])) "document-2.rps"))
     (is (= (take 2 @(rf/subscribe [::document.subs/recent])) ["foo/bar/document-2.rps"
                                                               "foo/bar/document-1.rps"])))))
