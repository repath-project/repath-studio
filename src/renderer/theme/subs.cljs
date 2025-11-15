(ns renderer.theme.subs
  (:require
   [re-frame.core :as rf]
   [renderer.theme.handlers :as theme.handlers]))

(rf/reg-sub
 ::theme
 (fn [db _]
   (:theme db)))

(rf/reg-sub
 ::mode
 :<- [::theme]
 :mode)

(rf/reg-sub
 ::selected-mode?
 :<- [::mode]
 (fn [mode [_ k]]
   (= mode k)))

(rf/reg-sub
 ::native-mode
 :<- [::theme]
 :native-mode)

(rf/reg-sub
 ::computed-mode
 :<- [::mode]
 :<- [::native-mode]
 (fn [[mode native-mode] _]
   (theme.handlers/compute-mode mode native-mode)))

(rf/reg-sub
 ::codemirror
 :<- [::computed-mode]
 (fn [computed-mode _]
   (if (= computed-mode :dark)
     "tomorrow-night-eighties"
     "default")))
