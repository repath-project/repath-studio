(ns renderer.theme.db)

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
