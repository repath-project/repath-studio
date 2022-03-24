(ns repath.studio.subs
  (:require
   [re-frame.core :as rf]))

(defn reg-basic-sub
  [k]
  (rf/reg-sub
   k
   (fn [db _]
     (get-in db [k]))))

(doseq [x [:active-theme
           :tool
           :state
           :left-sidebar-width
           :right-sidebar-width
           :tree?
           :properties?
           :header?
           :timeline?
           :history?
           :command-palette?
           :cursor
           :active-document
           :documents
           :document-tabs
           :mouse-pos
           :mouse-over-canvas?
           :offset
           :content-rect
           :copied-elements
           :system-fonts
           :overlay
           :window/minimized?
           :window/maximized?
           :window/fullscreen?
           :debug-info?
           :elements-collapsed?
           :pages-collapsed?
           :defs-collapsed?
           :repl-history-collapsed?
           :symbols-collapsed?
           :color-palette]] (reg-basic-sub x))

(rf/reg-sub
 :font-options
 :<- [:system-fonts]
 (fn [system-fonts _]
   (mapv (fn [font] {:key (keyword font)
                     :text font
                     :styles {:optionText {:fontFamily font :font-size "14px"}}}) system-fonts)))
