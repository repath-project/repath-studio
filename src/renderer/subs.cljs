(ns renderer.subs
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]))

(rf/reg-sub
 :tool
 (fn [db _]
   (:tool db)))

(rf/reg-sub
 :primary-tool
 (fn [db _]
   (:primary-tool db)))

(rf/reg-sub
 :mouse-pos
 (fn [db _]
   (:mouse-pos db)))

(rf/reg-sub
 :adjusted-mouse-pos
 (fn [db _]
   (:adjusted-mouse-pos db)))

(rf/reg-sub
 :adjusted-mouse-offset
 (fn [db _]
   (:adjusted-mouse-offset db)))

(rf/reg-sub
 :mouse-offset
 (fn [db _]
   (:mouse-offset db)))

(rf/reg-sub
 :drag?
 (fn [db _]
   (:drag? db)))

(rf/reg-sub
 :cursor
 (fn [db _]
   (:cursor db)))

(rf/reg-sub
 :state
 (fn [db _]
   (:state db)))

(rf/reg-sub
 :message
 (fn [db _]
   (:message db)))

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
 :webref-css
 (fn [db _]
   (:webref-css db)))

(rf/reg-sub
 :webref-css-property
 :<- [:webref-css]
 (fn [webref-css [_ property]]
   (some
    #(when (= (:name %) (name property)) %)
    (flatten (map (fn [[_ item]] (:properties item)) webref-css)))))

(rf/reg-sub
 :mdn
 (fn [db _]
   (:mdn db)))

(rf/reg-sub
 :css-property
 :<- [:mdn]
 (fn [mdn [_ property]]
   (get-in mdn [:css :properties property])))

(rf/reg-sub
 :backdrop?
 (fn [db _]
   (:backdrop? db)))

(rf/reg-sub
 :debug-info?
 (fn [db _]
   (:debug-info? db)))

(rf/reg-sub
 :clicked-element
 (fn [db _]
   (:clicked-element db)))

(rf/reg-sub
 :repl-mode
 (fn [db _]
   (:repl-mode db)))

(rf/reg-sub
 :shortcuts
 (fn [db _]
   (-> db ::rp/keydown :event-keys)))

(rf/reg-sub
 :event-shortcuts
 :<- [:shortcuts]
 (fn [shortcuts [_ event]]
   (->> shortcuts
        (filter #(= (first %) event))
        (first)
        (rest))))

#_(rf/reg-sub
   :font-options
   :<- [:system-fonts]
   (fn [system-fonts _]
     (mapv (fn [font]
             {:key (keyword font)
              :text font
              :styles {:optionText {:fontFamily font
                                    :font-size "14px"}}})
           system-fonts)))
