(ns renderer.app.subs
  (:require
   [re-frame.core :as rf]
   [renderer.app.handlers :as app.handlers]
   [renderer.utils.i18n :as utils.i18n]))

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
 ::font-list
 :<- [::system-fonts]
 (fn [system-fonts _]
   (some->> system-fonts
            keys
            sort)))

(rf/reg-sub
 ::backdrop
 :-> :backdrop)

(rf/reg-sub
 ::debug-info
 :-> :debug-info)

(rf/reg-sub
 ::help-bar
 :-> :help-bar)

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
 ::system-lang
 :-> :system-lang)

(rf/reg-sub
 ::computed-lang
 :<- [::lang]
 :<- [::system-lang]
 (fn [[lang system-lang] _]
   (utils.i18n/computed-lang lang system-lang)))

(rf/reg-sub
 ::lang-dir
 :<- [::computed-lang]
 (fn [computed-lang _]
   (get-in utils.i18n/languages [computed-lang :dir])))

(rf/reg-sub
 ::platform
 :-> :platform)

(rf/reg-sub
 ::web?
 :<- [::platform]
 (fn [platform _]
   (app.handlers/web? platform)))

(rf/reg-sub
 ::desktop?
 :<- [::platform]
 (fn [platform _]
   (app.handlers/desktop? platform)))

(rf/reg-sub
 ::mobile?
 :<- [::platform]
 (fn [platform _]
   (app.handlers/mobile? platform)))

(rf/reg-sub
 ::mac?
 :<- [::platform]
 (fn [platform _]
   (= platform "darwin")))

(rf/reg-sub
 ::standalone?
 :-> :standalone)

(rf/reg-sub
 ::install-prompt
 :-> :install-prompt)

(rf/reg-sub
 ::installable?
 :<- [::web?]
 :<- [::standalone?]
 :<- [::install-prompt]
 (fn [[web? standalone? install-prompt] _]
   (and web? (not standalone?) install-prompt)))

(rf/reg-sub
 ::user-agent
 :-> :user-agent)

(rf/reg-sub
 ::grid
 :-> :grid)

(rf/reg-sub
 ::loading?
 :-> :loading)

(rf/reg-sub
 ::panel-visible?
 (fn [db [_ k]]
   (-> db :panels k :visible)))

(rf/reg-sub
 ::features
 :-> :features)

(rf/reg-sub
 ::feature?
 :<- [::features]
 (fn [features [_ k]]
   (contains? features k)))

(rf/reg-sub
 ::menubar
 :-> :menubar)

(rf/reg-sub
 ::menubar-indicator?
 :<- [::menubar]
 :-> :indicator)

(rf/reg-sub
 ::menubar-active
 :<- [::menubar]
 :-> :active)
