(ns renderer.window.effects
  (:require
   [re-frame.core :as rf]
   [renderer.notification.events :as-alias notification.e]
   [renderer.utils.system :as system]))

(rf/reg-fx
 ::close
 (fn [_]
   (.close js/window)))

(rf/reg-fx
 ::relaunch
 (fn [_]
   (.reload js/window.location)))

(rf/reg-fx
 ::toggle-fullscreen
 (fn [_]
   (if (.-fullscreenElement js/document)
     (.exitFullscreen js/document)
     (.. js/document -documentElement requestFullscreen))))

(rf/reg-fx
 ::open-remote-url
 (fn [url]
   (.open js/window url)))

(rf/reg-fx
 ::ipc-send
 (fn [[channel data]]
   (when system/electron?
     (js/window.api.send channel (clj->js data)))))

(rf/reg-fx
 ::ipc-invoke
 (fn [{:keys [channel data formatter on-success on-error]}]
   (when system/electron?
     (-> (js/window.api.invoke channel (clj->js data))
         (.then #(when on-success (rf/dispatch (conj on-success (cond-> % formatter formatter)))))
         (.catch #(when on-error (rf/dispatch (conj on-error %))))))))
