(ns renderer.window.effects
  (:require
   [platform :as platform]
   [promesa.core :as p]
   [re-frame.core :as rf]))

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
   (js/window.api.send channel (clj->js data))))

(rf/reg-fx
 ::ipc-invoke
 (fn [{:keys [channel data formatter on-resolution]}]
   (p/let [result (js/window.api.invoke channel (clj->js data))]
     (when on-resolution
       (rf/dispatch [on-resolution (cond-> result formatter formatter)])))))
