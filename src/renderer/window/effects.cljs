(ns renderer.window.effects
  (:require
   [re-frame.core :as rf]))

(rf/reg-fx
 ::close
 (fn [_]
   (.close js/window)))

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
