(ns renderer.app.subs
  (:require
   ["mdn-data" :as mdn]
   [camel-snake-kebab.core :as csk]
   [clojure.string :as str]
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
 ::drag
 :-> :drag)

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
   (let [dispatch-state (if (contains? (methods tool.hierarchy/help) [tool state])
                          state
                          :idle)]
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

(defonce mdn-data (js->clj mdn :keywordize-keys true))

(rf/reg-sub
 ::property
 :<- [::webref-css]
 (fn [webref-css [_ property]]
   ;; Mdn is deprecated in favor of w3c/webref, but w3c/webref is not available in browsers.
   ;; The data is similar but not exactly the same, so we merge them below.
   (let [webref-css-property (some
                              #(when (= (:name %) (name property)) %)
                              (flatten (map (fn [[_ item]] (:properties item)) webref-css)))
         css-property (-> (get-in mdn-data [:css :properties property])
                          (update-keys  #(case %
                                           :appliesto :appliesTo
                                           :computed :computedValue
                                           %)))
         enhance-readability (fn [property k]
                               (cond-> property
                                 (and (get property k) (string? (get property k)))
                                 (update k #(-> % csk/->kebab-case-string (str/replace "-" " ")))))
         css-property (reduce enhance-readability css-property [:appliesTo :computedValue :percentages])]
     (-> css-property
         (merge webref-css-property)
         (enhance-readability :animationType)))))

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
 ::panel-visible
 (fn [db [_ k]]
   (-> db :panels k :visible)))

(rf/reg-sub
 ::font-options
 :<- [::system-fonts]
 (fn [system-fonts _]
   (->> system-fonts
        (map :family)
        (distinct))))
