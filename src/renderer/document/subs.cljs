(ns renderer.document.subs
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.document.handlers :as h]
   [renderer.timeline.subs :as-alias timeline.s]))

(rf/reg-sub
 ::active-id
 :-> :active-document)

(rf/reg-sub
 ::entities
 :-> :documents)

(rf/reg-sub
 ::tabs
 :-> :document-tabs)

(rf/reg-sub
 ::recent
 (fn [db _]
   (-> db :recent reverse)))

(rf/reg-sub
 ::some
 :<- [::entities]
 seq)

(rf/reg-sub
 ::some-recent
 :<- [::recent]
 seq)

(rf/reg-sub
 ::active
 :<- [::entities]
 :<- [::active-id]
 (fn [[documents active-document] _]
   (when active-document
     (get documents active-document))))

(rf/reg-sub
 ::entity
 :<- [::entities]
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
 ::attrs
 :<- [::active]
 :-> :attrs)

(rf/reg-sub
 ::fill
 :<- [::attrs]
 :-> :fill)

(rf/reg-sub
 ::stroke
 :<- [::attrs]
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
 :<- [::active-saved]
 (fn [[title path active saved] _]
   (let [title (or path title)]
     (str (when (and active (not saved)) "• ")
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
 ::filter-active
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
 ::read-only
 :<- [::timeline.s/time]
 pos?)

(rf/reg-sub
 ::saved
 (fn [db [_ id]]
   (h/saved? db id)))

(rf/reg-sub
 ::active-saved
 (fn [{:keys [active-document] :as db} [_]]
   (when active-document
     (h/saved? db active-document))))
