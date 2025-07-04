(ns renderer.tool.subs
  (:require
   [re-frame.core :as rf]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(rf/reg-sub
 ::active
 :-> :tool)

(rf/reg-sub
 ::cached
 :-> :cached-tool)

(rf/reg-sub
 ::pivot-point
 :-> :pivot-point)

(rf/reg-sub
 ::drag?
 :-> :drag)

(rf/reg-sub
 ::cursor
 :-> :cursor)

(rf/reg-sub
 ::state
 :-> :state)

(rf/reg-sub
 ::cached-state
 :-> :cached-state)

(rf/reg-sub
 ::help
 :<- [::active]
 :<- [::state]
 (fn [[tool state] _]
   (let [dispatch-state (if (contains? (methods tool.hierarchy/help) [tool state])
                          state
                          :idle)]
     (tool.hierarchy/help tool dispatch-state))))
