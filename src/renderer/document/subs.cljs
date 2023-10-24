(ns renderer.document.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :document/active
 :<- [:documents]
 :<- [:active-document]
 (fn [[documents active-document] _]
   (when active-document
     (active-document documents))))

(rf/reg-sub
 :document/zoom
 :<- [:document/active]
 (fn [document _]
   (:zoom document)))

(rf/reg-sub
 :document/rotate
 :<- [:document/active]
 (fn [document _]
   (:rotate document)))

(rf/reg-sub
 :document/fill
 :<- [:document/active]
 (fn [document _]
   (:fill document)))

(rf/reg-sub
 :document/stroke
 :<- [:document/active]
 (fn [document _]
   (:stroke document)))

(rf/reg-sub
 :document/pan
 :<- [:document/active]
 (fn [document _]
   (:pan document)))

(rf/reg-sub
 :document/title
 :<- [:document/active]
 (fn [document _]
   (:title document)))

(rf/reg-sub
 :document/history
 :<- [:document/active]
 (fn [document _]
   (:history document)))

(rf/reg-sub
 :document/elements
 :<- [:document/active]
 (fn [document _]
   (:elements document)))

(rf/reg-sub
 :document/active-page
 :<- [:document/active]
 (fn [document _]
   (:active-page document)))

(rf/reg-sub
 :document/temp-element
 :<- [:document/active]
 (fn [document _]
   (:temp-element document)))

(rf/reg-sub
 :document/filter
 :<- [:document/active]
 (fn [document _]
   (:filter document)))

(rf/reg-sub
 :document/hovered-keys
 :<- [:document/active]
 (fn [document _]
   (:hovered-keys document)))

(rf/reg-sub
 :document/ignored-keys
 :<- [:document/active]
 (fn [document _]
   (:ignored-keys document)))

#_(rf/reg-sub
   :document/rulers-locked?
   :<- [:document/active]
   (fn [document _]
     (:rulers-locked? document)))

(rf/reg-sub
 :document/rulers?
 :<- [:document/active]
 (fn [document _]
   (:rulers? document)))

(rf/reg-sub
 :document/xml?
 :<- [:document/active]
 (fn [document _]
   (:xml? document)))

(rf/reg-sub
 :document/history?
 :<- [:document/active]
 (fn [document _]
   (:history? document)))

(rf/reg-sub
 :document/grid?
 :<- [:document/active]
 (fn [document _]
   (:grid? document)))

#_(rf/reg-sub
   :document/snap?
   :<- [:document/active]
   (fn [document _]
     (:snap? document)))