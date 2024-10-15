(ns renderer.theme.db)

;; The iframe is isolated so we don't have access to the css vars of the parent.
;; We are currently using hardcoded values, but we hould be able to set those
;; vars in the nested document if we have to.
(def accent-inverted "#fff")
(def accent "#e93976")
(def font-mono "'Consolas (Custom)', 'Bitstream Vera Sans Mono', monospace, 'Apple Color Emoji', 'Segoe UI Emoji'")
(def handle-size 12)
(def dash-size 5)

(def Theme
  [:map {:default {} :closed true}
   [:mode {:default :dark} [:enum :dark :light :system]]
   [:native-mode {:optional true} [:enum :dark :light]]])
