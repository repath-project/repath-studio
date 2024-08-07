(ns renderer.theme.effects
  (:require
   [re-frame.core :as rf]))

(rf/reg-fx
 ::set-document-attr
 (fn [[mode]]
   (.setAttribute js/window.document.documentElement "data-theme" mode)))
