(ns repath.studio.documents.subs
  (:require
   [re-frame.core :as rf]))

(defn reg-document-sub
  [k]
  (rf/reg-sub
   k
   :<- [:documents]
   :<- [:active-document]
   (fn [[documents active-document] _]
     (get-in documents [active-document k]))))

(doseq [x [:zoom
           :rotate
           :fill
           :stroke
           :stroke-width
           :active-page
           :pan
           :title
           :history
           :elements
           :temp-element
           :filter
           :hovered-keys
           :selected-keys
           :rulers-locked?
           :rulers?
           :grid?]] (reg-document-sub x))

(rf/reg-sub
 :document
 (fn [db [_ key]]
   (get-in db [:documents key])))
