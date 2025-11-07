(ns renderer.a11y.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::filters
 :-> :a11y-filters)
