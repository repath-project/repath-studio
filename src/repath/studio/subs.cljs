(ns repath.studio.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :active-theme
 (fn [db _]
   (:active-theme db)))

(rf/reg-sub
 :tool
 (fn [db _]
   (:tool db)))

(rf/reg-sub
 :cached-tool
 (fn [db _]
   (:cached-tool db)))


(rf/reg-sub
 :mouse-pos
 (fn [db _]
   (:mouse-pos db)))

(rf/reg-sub
 :cursor
 (fn [db _]
   (:cursor db)))

(rf/reg-sub
 :state
 (fn [db _]
   (:state db)))

(rf/reg-sub
 :command-palette?
 (fn [db _]
   (:command-palette? db)))

(rf/reg-sub
 :active-document
 (fn [db _]
   (:active-document db)))

(rf/reg-sub
 :documents
 (fn [db _]
   (:documents db)))

(rf/reg-sub
 :document-tabs
 (fn [db _]
   (:document-tabs db)))

(rf/reg-sub
 :offset
 (fn [db _]
   (:offset db)))

(rf/reg-sub
 :content-rect
 (fn [db _]
   (:content-rect db)))

(rf/reg-sub
 :copied-elements
 (fn [db _]
   (:copied-elements db)))

(rf/reg-sub
 :system-fonts
 (fn [db _]
   (:system-fonts db)))

(rf/reg-sub
 :overlay
 (fn [db _]
   (:overlay db)))

(rf/reg-sub
 :debug-info?
 (fn [db _]
   (:debug-info? db)))

(rf/reg-sub
 :color-palette
 (fn [db _]
   (:color-palette db)))

(rf/reg-sub
 :font-options
 :<- [:system-fonts]
 (fn [system-fonts _]
   (mapv (fn [font] {:key (keyword font)
                     :text font
                     :styles {:optionText {:fontFamily font :font-size "14px"}}}) system-fonts)))
