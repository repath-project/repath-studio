(ns renderer.dialog.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::entities
 :-> :dialogs)
