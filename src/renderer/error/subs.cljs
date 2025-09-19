(ns renderer.error.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::reporting?
 :-> :error-reporting)
