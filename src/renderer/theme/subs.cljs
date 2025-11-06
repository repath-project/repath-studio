(ns renderer.theme.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::theme
 (fn [db _]
   (:theme db)))

(rf/reg-sub
 ::mode
 :<- [::theme]
 :mode)

(rf/reg-sub
 ::native-mode
 :<- [::theme]
 :native-mode)

(rf/reg-sub
 ::codemirror
 :<- [::mode]
 :<- [::native-mode]
 (fn [[mode native-mode] _]
   (let [mode (if (= mode :system) native-mode mode)]
     (if (= mode :dark)
       "tomorrow-night-eighties"
       "default"))))
