(ns renderer.theme.db)

(def handle-size 13)

(def modes
  [:dark :light :system])

(def ThemeMode
  (into [:enum] modes))

(def Theme
  [:map {:closed true}
   [:mode {:default :dark} ThemeMode]])
