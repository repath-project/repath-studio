(ns renderer.app.subs
  (:require
   [re-frame.core :as rf]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(rf/reg-sub
 ::tool
 :-> :tool)

(rf/reg-sub
 ::primary-tool
 :-> :primary-tool)

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
 ::pivot-point
 :-> :pivot-point)

(rf/reg-sub
 ::drag?
 :-> :drag?)

(rf/reg-sub
 ::ruler-size
 :-> :ruler-size)

(rf/reg-sub
 ::loading?
 :-> :loading?)

(rf/reg-sub
 ::cursor
 :-> :cursor)

(rf/reg-sub
 ::state
 :-> :state)

(rf/reg-sub
 ::help
 :<- [::tool]
 :<- [::state]
 (fn [[tool state] _]
   (let [dispatch-state (if (contains? (methods tool.hierarchy/help) [tool state]) state :default)]
     (tool.hierarchy/help tool dispatch-state))))

(rf/reg-sub
 ::active-document
 :-> :active-document)

(rf/reg-sub
 ::documents
 :-> :documents)

(rf/reg-sub
 ::document-tabs
 :-> :document-tabs)

(rf/reg-sub
 ::dom-rect
 :-> :dom-rect)

(rf/reg-sub
 ::system-fonts
 :-> :system-fonts)

(rf/reg-sub
 ::webref-css
 :-> :webref-css)

(rf/reg-sub
 ::mdn
 :-> :mdn)

(rf/reg-sub
 ::property
 :<- [::webref-css]
 :<- [::mdn]
 (fn [[webref-css mdn] [_ property]]
   ;; Mdn is deprecated in favor of w3c/webref, but w3c/webref is not available in browsers.
   ;; The data is similar but not exactly the same, so we merge them below.
   (let [webref-css-property (some
                              #(when (= (:name %) (name property)) %)
                              (flatten (map (fn [[_ item]] (:properties item)) webref-css)))
         css-property (get-in mdn [:css :properties property])
         css-property (update-keys css-property #(case %
                                                   :appliesto :appliesTo
                                                   :computed :computedValue
                                                   %))]
     (merge css-property webref-css-property))))

(rf/reg-sub
 ::backdrop?
 :-> :backdrop?)

(rf/reg-sub
 ::debug-info?
 :-> :debug-info?)

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
 ::rulers-locked?
 :-> :rulers-locked?)

(rf/reg-sub
 ::rulers-visible?
 :-> :rulers-visible?)

(rf/reg-sub
 ::grid-visible?
 :-> :grid-visible?)

(rf/reg-sub
 ::panel-visible?
 (fn [db [_ k]]
   (-> db :panels k :visible?)))

(rf/reg-sub
 ::font-options
 :<- [::system-fonts]
 (fn [system-fonts _]
   (->> system-fonts
        (map :family)
        (distinct))))
