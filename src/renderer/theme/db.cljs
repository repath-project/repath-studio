(ns renderer.theme.db)

(def handle-size 13)

(def modes
  [:dark :light :system])

(def ThemeMode
  (into [:enum] modes))

(def NativeMode
  [:enum :dark :light])

(def Theme
  [:map {:closed true}
   [:mode {:default :system} ThemeMode]
   [:native-mode {:optional true} NativeMode]])
