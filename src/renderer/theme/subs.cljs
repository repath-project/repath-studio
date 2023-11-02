(ns renderer.theme.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :theme/mode
 :-> :theme-mode)