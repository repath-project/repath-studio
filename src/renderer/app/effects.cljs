(ns renderer.app.effects
  (:require
   ["localforage" :as localforage]
   [cognitect.transit :as transit]
   [config :as config]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [renderer.app.db :as app.db]
   [renderer.history.handlers :as history.handlers]
   [renderer.notification.events :as-alias notification.events]))

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
 ::language
 (fn [coeffects _]
   (assoc coeffects :language (.-language js/navigator))))

(rf/reg-fx
 ::query-local-fonts
 (fn [{:keys [on-success on-error formatter]}]
   (when-not (undefined? js/window.queryLocalFonts)
     (-> (.queryLocalFonts js/window)
         (.then #(when on-success (rf/dispatch (conj on-success (cond-> %
                                                                  formatter formatter)))))
         (.catch #(when on-error (rf/dispatch (conj on-error %))))))))

(defn json->clj
  [json]
  (transit/read (transit/reader :json) json))

(rf/reg-fx
 ::get-local-db
 (fn [{:keys [on-success on-error on-finally]}]
   (-> (localforage/getItem config/app-name)
       (.then #(when on-success (rf/dispatch (conj on-success (json->clj %)))))
       (.catch #(when on-error (rf/dispatch (conj on-error %))))
       (.finally #(when on-finally (rf/dispatch on-finally))))))

(rf/reg-fx
 ::persist
 (fn []
   (let [db (history.handlers/drop-rest @rf.db/app-db)
         persisted-db (select-keys db app.db/persisted-keys)
         writer (transit/writer :json)]
     (when-let [json (try (transit/write writer persisted-db)
                          (catch :default e
                            (rf/dispatch [::notification.events/show-exception e])))]
       (-> (localforage/setItem config/app-name json)
           (.catch #(rf/dispatch [::notification.events/show-exception %])))))))

(rf/reg-fx
 ::validate
 (fn [[db event]]
   (when (not (app.db/valid? db))
     (js/console.error (str "Event: " (first event)))
     (throw (js/Error. (str "Spec check failed: " (app.db/explain db)))))))

(rf/reg-fx
 ::clear-local-storage
 (fn []
   (localforage/clear)))
