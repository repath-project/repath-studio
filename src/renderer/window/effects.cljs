(ns renderer.window.effects
  (:require
   [re-frame.core :as rf]
   [renderer.utils.dom :as utils.dom]))

(rf/reg-cofx
 ::focused
 (fn [coeffects _]
   (assoc coeffects :focused
          (boolean (or (.hasFocus js/document)
                       (when-let [frame-document (utils.dom/frame-document!)]
                         (.hasFocus frame-document)))))))

(rf/reg-cofx
 ::fullscreen
 (fn [coeffects _]
   (assoc coeffects :fullscreen (boolean (.-fullscreenElement js/document)))))

(rf/reg-fx
 ::close
 (fn [_]
   (.close js/window)))

(rf/reg-fx
 ::reload
 (fn [_]
   (.reload js/window.location)))

(rf/reg-fx
 ::toggle-fullscreen
 (fn [_]
   (if (.-fullscreenElement js/document)
     (.exitFullscreen js/document)
     (.. js/document -documentElement requestFullscreen))))
