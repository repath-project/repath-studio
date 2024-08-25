(ns renderer.document.subs
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.handlers :as h]
   [renderer.timeline.subs :as-alias timeline.s]))

(rf/reg-sub
 ::recent
 (fn [db _]
   (-> db :recent reverse)))

(rf/reg-sub
 ::documents?
 :<- [::app.s/documents]
 (fn [documents _]
   (seq documents)))

(rf/reg-sub
 ::recent?
 :<- [::recent]
 (fn [recent _]
   (seq recent)))

(rf/reg-sub
 ::active
 :<- [::app.s/documents]
 :<- [::app.s/active-document]
 (fn [[documents active-document] _]
   (when active-document
     (get documents active-document))))

(rf/reg-sub
 ::document
 :<- [::app.s/documents]
 (fn [documents [_ k]]
   (get documents k)))

(rf/reg-sub
 ::zoom
 :<- [::active]
 :-> :zoom)

(rf/reg-sub
 ::rotate
 :<- [::active]
 :-> :rotate)

(rf/reg-sub
 ::fill
 :<- [::active]
 :-> :fill)

(rf/reg-sub
 ::stroke
 :<- [::active]
 :-> :stroke)

(rf/reg-sub
 ::pan
 :<- [::active]
 :-> :pan)

(rf/reg-sub
 ::title
 :<- [::active]
 :-> :title)

(rf/reg-sub
 ::path
 :<- [::active]
 :-> :path)

(rf/reg-sub
 ::title-bar
 :<- [::title]
 :<- [::path]
 :<- [::active]
 :<- [::active-saved?]
 (fn [[title path active saved?] _]
   (let [title (or path title)]
     (str (when (and active (not saved?)) "• ")
          (when title (str title " - "))
          config/app-name))))

(rf/reg-sub
 ::elements
 :<- [::active]
 :-> :elements)

(rf/reg-sub
 ::temp-element
 :<- [::active]
 :-> :temp-element)

(rf/reg-sub
 ::filter
 :<- [::active]
 :-> :filter)

(rf/reg-sub
 ::filter-active?
 :<- [::filter]
 (fn [active-filter [_ k]]
   (= active-filter k)))

(rf/reg-sub
 ::hovered-ids
 :<- [::active]
 :-> :hovered-ids)

(rf/reg-sub
 ::collapsed-ids
 :<- [::active]
 :-> :collapsed-ids)

(rf/reg-sub
 ::ignored-ids
 :<- [::active]
 :-> :ignored-ids)

(rf/reg-sub
 ::save
 :<- [::active]
 :-> :save)

(rf/reg-sub
 ::read-only?
 :<- [::timeline.s/time]
 (fn [t _]
   (pos? t)))

(rf/reg-sub
 ::saved?
 (fn [db [_ id]]
   (h/saved? db id)))

(rf/reg-sub
 ::active-saved?
 (fn [{:keys [active-document] :as db} [_]]
   (when active-document
     (h/saved? db active-document))))
