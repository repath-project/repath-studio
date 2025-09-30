(ns fixtures
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.effects :as-alias effects]
   [renderer.error.effects :as-alias error.effects]
   [renderer.theme.effects :as-alias theme.effects]
   [renderer.window.effects :as-alias window.effects]))

(def local-store
  (atom {config/app-name {:error-reporting false
                          :recent [{:id #uuid "123e4567-e89b-12d3-a456-426614174000"
                                    :title "drawing-1.rps"}]}}))

(defn test-fixtures
  []
  (rf/reg-fx
   ::effects/add-event-listener
   (fn [_]))

  (rf/reg-fx
   ::app.effects/get-local-store
   (fn [{:keys [store-key on-success on-finally]}]
     (some-> on-success (conj (get @local-store store-key)) rf/dispatch)
     (some-> on-finally rf/dispatch)))

  (rf/reg-fx
   ::app.effects/set-local-store
   (fn [{:keys [store-key data on-success]}]
     (swap! local-store assoc store-key data)
     (some-> on-success (conj data) rf/dispatch)))

  (rf/reg-fx
   ::app.effects/local-store-keys
   (fn [{:keys [on-success]}]
     (some-> on-success (conj (keys @local-store)) rf/dispatch)))

  (rf/reg-fx
   ::app.effects/remove-local-store
   (fn [{:keys [store-key]}]
     (swap! local-store dissoc store-key)))

  (rf/reg-fx
   ::app.effects/persist
   (fn []))

  (rf/reg-cofx
   ::window.effects/fullscreen
   (fn [coeffects _]
     (assoc coeffects :fullscreen true)))

  (rf/reg-cofx
   ::window.effects/focused
   (fn [coeffects _]
     (assoc coeffects :focused false)))

  (rf/reg-cofx
   ::app.effects/language
   (fn [coeffects _]
     (assoc coeffects :language "en-US")))

  (rf/reg-cofx
   ::theme.effects/native-mode
   (fn [coeffects _]
     (assoc coeffects :native-mode :light)))

  (rf/reg-fx
   ::theme.effects/add-listener
   (fn [_]))

  (rf/reg-fx
   ::error.effects/init-reporting
   (fn [_]))

  (rf/reg-fx
   ::app.effects/query-local-fonts
   (fn [{:keys [on-success formatter]}]
     (let [font-data (clj->js [{:family "Noto Sans"
                                :fullName "Noto Sans Regular"
                                :postscriptName "Noto-Sans-Regular"
                                :style "Regular"}
                               {:family "Adwaita Mono"
                                :fullName "Adwaita Mono Bold"
                                :postscriptName "Adwaita-Mono-Bold"
                                :style "Bold"}
                               {:family "Adwaita Mono"
                                :fullName "Adwaita Mono Bold Italic"
                                :postscriptName "Adwaita-Mono-Bold-Italic"
                                :style "Bold Italic"}])]
       (rf/dispatch (conj on-success (cond-> font-data
                                       formatter formatter))))))

  (rf/reg-cofx
   ::app.effects/features
   (fn [coeffects _]
     (assoc coeffects :features #{:file-system :local-fonts}))))
