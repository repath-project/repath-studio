(ns renderer.app.effects
  (:require
   [akiroz.re-frame.storage :as rf.storage]
   [config :as config]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [renderer.app.db :as app.db]
   [renderer.history.handlers :as history.handlers]))

(rf.storage/reg-co-fx! config/app-key {:cofx :store})

(rf/reg-cofx
 ::platform
 (fn [coeffects _]
   (assoc coeffects :platform (if js/window.api
                                js/window.api.platform
                                "web"))))

(rf/reg-cofx
 ::versions
 (fn [coeffects _]
   (cond-> coeffects
     js/window.api
     (assoc :versions js/window.api.versions))))

(rf/reg-cofx
 ::env
 (fn [coeffects _]
   (cond-> coeffects
     js/window.api
     (assoc :env js/window.api.env))))

(rf/reg-cofx
 ::user-agent
 (fn [coeffects _]
   (assoc coeffects :user-agent (.-userAgent js/navigator))))

(rf/reg-cofx
 ::system-language
 (fn [coeffects _]
   (assoc coeffects :system-language (.-language js/navigator))))

(rf/reg-fx
 ::query-local-fonts
 (fn [{:keys [on-success on-error formatter]}]
   (when-not (undefined? js/window.queryLocalFonts)
     (-> (.queryLocalFonts js/window)
         (.then #(when on-success (rf/dispatch (conj on-success
                                                     (cond-> %
                                                       formatter formatter)))))
         (.catch #(when on-error (rf/dispatch (conj on-error %))))))))

(rf/reg-fx
 ::persist
 (fn []
   (let [db @rf.db/app-db
         db (cond-> db
              (:active-document db)
              history.handlers/drop-rest)]
     (->> (select-keys db app.db/persisted-keys)
          (rf.storage/->store config/app-key)))))

(rf/reg-fx
 ::validate
 (fn [[db event]]
   (when (not (app.db/valid? db))
     (js/console.error (str "Event: " (first event)))
     (throw (js/Error. (str "Spec check failed: " (app.db/explain db)))))))

(rf/reg-fx
 ::clear-local-storage
 (fn []
   (rf.storage/->store config/app-key {})))
