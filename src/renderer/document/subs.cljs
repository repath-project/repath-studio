(ns renderer.document.subs
  (:require
   [re-frame.core :as rf]
   [renderer.document.handlers :as h]
   [renderer.timeline.subs :as-alias timeline.s]))

(rf/reg-sub
 ::recent
 (fn [db _]
   (-> db :recent reverse)))

(rf/reg-sub
 ::documents?
 :<- [:documents]
 (fn [documents _]
   (seq documents)))

(rf/reg-sub
 ::recent?
 :<- [::recent]
 (fn [recent _]
   (seq recent)))

(rf/reg-sub
 ::active
 :<- [:documents]
 :<- [:active-document]
 (fn [[documents active-document] _]
   (when active-document
     (active-document documents))))

(rf/reg-sub
 ::document
 :<- [:documents]
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
          "Repath Studio"))))

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
 ::hovered-keys
 :<- [::active]
 :-> :hovered-keys)

(rf/reg-sub
 ::collapsed-keys
 :<- [::active]
 :-> :collapsed-keys)

(rf/reg-sub
 ::ignored-keys
 :<- [::active]
 :-> :ignored-keys)

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
 (fn [db [_ k]]
   (h/saved? db k)))

(rf/reg-sub
 ::active-saved?
 (fn [{:keys [active-document] :as db} [_]]
   (when active-document
     (h/saved? db active-document))))
