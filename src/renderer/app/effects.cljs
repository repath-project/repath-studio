(ns renderer.app.effects
  (:require
   ["localforage" :as localforage]
   [cognitect.transit :as transit]
   [config :as config]
   [goog.functions]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [renderer.app.db :as app.db]
   [renderer.document.handlers :as document.handlers]
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
 ::standalone
 (fn [coeffects _]
   (assoc coeffects
          :standalone
          (or (.-standalone js/navigator)
              (.-matches (js/matchMedia "(display-mode: standalone"))))))

(rf/reg-cofx
 ::language
 (fn [coeffects _]
   (assoc coeffects :language (.-language js/navigator))))

(rf/reg-fx
 ::query-local-fonts
 (fn [{:keys [on-success on-error formatter]}]
   (when-not (undefined? js/window.queryLocalFonts)
     (-> (.queryLocalFonts js/window)
         (.then #(when on-success
                   (rf/dispatch (conj on-success (cond-> %
                                                   formatter formatter)))))
         (.catch #(when on-error (rf/dispatch (conj on-error %))))))))

(defn- json->clj
  [data]
  (-> (transit/reader :json)
      (transit/read data)))

(rf/reg-fx
 ::get-local-db
 (fn [{:keys [on-success on-error on-finally]}]
   (-> (localforage/getItem config/app-name)
       (.then #(when on-success (rf/dispatch (conj on-success (json->clj %)))))
       (.catch #(when on-error (rf/dispatch (conj on-error %))))
       (.finally #(when on-finally (rf/dispatch on-finally))))))

(defn persist
  []
  (let [db (-> @rf.db/app-db
               (history.handlers/drop-rest)
               (document.handlers/remove-file-handle))
        persisted-db (select-keys db app.db/persisted-keys)
        writer (transit/writer :json)]
    (when-let [json (try (transit/write writer persisted-db)
                         (catch :default err
                           (rf/dispatch [::notification.events/show-exception
                                         err])))]
      (-> (localforage/setItem config/app-name json)
          (.catch #(rf/dispatch [::notification.events/show-exception %]))))))

(def debounced-persist (goog.functions/debounce persist 500))

(rf/reg-fx
 ::persist
 debounced-persist)

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

(rf/reg-fx
 ::install
 (fn [{:keys [prompt outcomes]}]
   (.prompt prompt)
   (-> (.-userChoice prompt)
       (.then (fn [choice]
                (when-let [outcome-event (get outcomes (.-outcome choice))]
                  (rf/dispatch outcome-event)))))))
