(ns renderer.app.effects
  (:require
   [akiroz.re-frame.storage :as rf.storage]
   [config :as config]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [renderer.app.db :as app.db]
   [renderer.history.handlers :as history.handlers]))

(rf.storage/reg-co-fx! config/app-key {:cofx :store})

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
