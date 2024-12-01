(ns renderer.app.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::pointer-pos
 :-> :pointer-pos)

(rf/reg-sub
 ::adjusted-pointer-pos
 :-> :adjusted-pointer-pos)

(rf/reg-sub
 ::pointer-offset
 :-> :pointer-offset)

(rf/reg-sub
 ::adjusted-pointer-offset
 :-> :adjusted-pointer-offset)

(rf/reg-sub
 ::dom-rect
 :-> :dom-rect)

(rf/reg-sub
 ::system-fonts
 :-> :system-fonts)

(rf/reg-sub
 ::backdrop
 :-> :backdrop)

(rf/reg-sub
 ::debug-info
 :-> :debug-info)

(rf/reg-sub
 ::clicked-element
 :-> :clicked-element)

(rf/reg-sub
 ::repl-mode
 :-> :repl-mode)

(rf/reg-sub
 ::keydown-rules
 :-> :re-pressed.core/keydown)

(rf/reg-sub
 ::event-shortcuts
 :<- [::keydown-rules]
 (fn [keydown-rules [_ event]]
   (->> keydown-rules
        :event-keys
        (filter #(= (first %) event))
        (first)
        (rest))))

(rf/reg-sub
 ::lang
 :-> :lang)

(rf/reg-sub
 ::grid
 :-> :grid)

(rf/reg-sub
 ::panel-visible?
 (fn [db [_ k]]
   (-> db :panels k :visible)))

(rf/reg-sub
 ::font-options
 :<- [::system-fonts]
 (fn [system-fonts _]
   (->> system-fonts
        (map :family)
        (distinct))))
