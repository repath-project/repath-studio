(ns renderer.window.db)

(def window
  [:map {:default {}}
   [:maximized? [boolean? {:default true}]]
   [:minimized? [boolean? {:default false}]]
   [:fullscreen? [boolean? {:default false}]]
   [:focused? [boolean? {:default false}]]
   ;; REVIEW: Maybe we need this per document?
   [:focused-once? [boolean? {:default false}]]])
