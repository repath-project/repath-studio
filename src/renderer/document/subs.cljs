(ns renderer.document.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :document/recent
 :-> :recent)

(rf/reg-sub
 :document/recent?
 :<- [:document/recent]
 (fn [recent _]
   (seq recent)))

(rf/reg-sub
 :document/recent-disabled?
 :<- [:document/recent?]
 (fn [recent? _]
   (not recent?)))

(rf/reg-sub
 :document/active
 :<- [:documents]
 :<- [:active-document]
 (fn [[documents active-document] _]
   (when active-document
     (active-document documents))))

(rf/reg-sub
 :document/document
 :<- [:documents]
 (fn [documents [_ k]]
   (get documents k)))

(rf/reg-sub
 :document/zoom
 :<- [:document/active]
 :-> :zoom)

(rf/reg-sub
 :document/rotate
 :<- [:document/active]
 :-> :rotate)

(rf/reg-sub
 :document/fill
 :<- [:document/active]
 :-> :fill)

(rf/reg-sub
 :document/stroke
 :<- [:document/active]
 :-> :stroke)

(rf/reg-sub
 :document/pan
 :<- [:document/active]
 :-> :pan)

(rf/reg-sub
 :document/title
 :<- [:document/active]
 :-> :title)

(rf/reg-sub
 :document/path
 :<- [:document/active]
 :-> :path)

(rf/reg-sub
 :document/title-bar
 :<- [:document/title]
 :<- [:document/path]
 (fn [[title path] _]
   (let [title (or path title)]
     (when title (str title " - ")) "Repath Studio")))

(rf/reg-sub
 :document/elements
 :<- [:document/active]
 :-> :elements)

(rf/reg-sub
 :document/temp-element
 :<- [:document/active]
 :-> :temp-element)

(rf/reg-sub
 :document/filter
 :<- [:document/active]
 :-> :filter)

(rf/reg-sub
 :document/hovered-keys
 :<- [:document/active]
 :-> :hovered-keys)

(rf/reg-sub
 :document/collapsed-keys
 :<- [:document/active]
 :-> :collapsed-keys)

(rf/reg-sub
 :document/ignored-keys
 :<- [:document/active]
 :-> :ignored-keys)

(rf/reg-sub
 :document/save
 :<- [:document/active]
 :-> :save)

(rf/reg-sub
 :document/read-only?
 :<- [:timeline/time]
 (fn [time _]
   (pos? time)))

(rf/reg-sub
 :document/saved?
 :<- [:documents]
 (fn [documents [_ k]]
   (= (get-in documents [k :save])
      (get-in documents [k :history :position]))))

(rf/reg-sub
 :document/active-saved?
 :<- [:document/active]
 (fn [document [_]]
   (= (:save document)
      (get-in document [:history :position]))))
