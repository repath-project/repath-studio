(ns renderer.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :tool
 :-> :tool)

(rf/reg-sub
 :primary-tool
 :-> :primary-tool)

(rf/reg-sub
 :mouse-pos
 :-> :mouse-pos)

(rf/reg-sub
 :adjusted-mouse-pos
 :-> :adjusted-mouse-pos)

(rf/reg-sub
 :adjusted-mouse-offset
 :-> :adjusted-mouse-offset)

(rf/reg-sub
 :mouse-offset
 :-> :mouse-offset)

(rf/reg-sub
 :drag?
 :-> :drag?)

(rf/reg-sub
 :cursor
 :-> :cursor)

(rf/reg-sub
 :state
 :-> :state)

(rf/reg-sub
 :message
 :-> :message)

(rf/reg-sub
 :command-palette?
 :-> :command-palette?)

(rf/reg-sub
 :active-document
 :-> :active-document)

(rf/reg-sub
 :documents
 :-> :documents)

(rf/reg-sub
 :document-tabs
 :-> :document-tabs)

(rf/reg-sub
 :offset
 :-> :offset)

(rf/reg-sub
 :content-rect
 :-> :content-rect)

(rf/reg-sub
 :copied-elements
 :-> :copied-elements)

(rf/reg-sub
 :system-fonts
 :-> :system-fonts)

(rf/reg-sub
 :webref-css
 :-> :webref-css)

(rf/reg-sub
 :webref-css-property
 :<- [:webref-css]
 (fn [webref-css [_ property]]
   (some
    #(when (= (:name %) (name property)) %)
    (flatten (map (fn [[_ item]] (:properties item)) webref-css)))))

(rf/reg-sub
 :mdn
 :-> :mdn)

(rf/reg-sub
 :css-property
 :<- [:mdn]
 (fn [mdn [_ property]]
   (get-in mdn [:css :properties property])))

(rf/reg-sub
 :backdrop?
 :-> :backdrop?)

(rf/reg-sub
 :debug-info?
 :-> :debug-info?)

(rf/reg-sub
 :clicked-element
 :-> :clicked-element)

(rf/reg-sub
 :repl-mode
 :-> :repl-mode)

(rf/reg-sub
 :keydown-rules
 :-> :re-pressed.core/keydown)

(rf/reg-sub
 :event-shortcuts
 :<- [:keydown-rules]
 (fn [keydown-rules [_ event]]
   (->> keydown-rules
        :event-keys
        (filter #(= (first %) event))
        (first)
        (rest))))

#_(rf/reg-sub
   :font-options
   :<- [:system-fonts]
   (fn [system-fonts _]
     (mapv (fn [font]
             {:key (keyword font)
              :text font
              :styles {:optionText {:fontFamily font
                                    :font-size "14px"}}})
           system-fonts)))