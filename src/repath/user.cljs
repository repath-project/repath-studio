(ns repath.user
  (:require
   [re-frame.core :as rf]
   [re-frame.db :as db]))

(defn translate
  "Moves the selected elements."
  ([offset]
   (rf/dispatch [:elements/translate offset])
   "")
  ([x y]
   (translate [x y])))

(defn fill
  "Fills the selected elements."
  [color]
  (rf/dispatch [:elements/fill color])
  "")

(defn delete
  "Deletes selected elements."
  []
   (rf/dispatch [:elements/delete]))

(defn copy
  "Copies the selected elements."
  []
  (rf/dispatch [:elements/copy])
  "")

(defn paste
  "Pastes the selected elements."
  []
  (rf/dispatch [:elements/paste])
  "")

(defn paste-in-place
  "Pastes the selected elements in place."
  []
  (rf/dispatch [:elements/paste-in-place])
  "")

(defn duplicate
  "Duplicates the selected elements."
  []
  (rf/dispatch [:elements/duplicate-in-posistion])
  "")

(defn create
  "Creates a new element."
  [element]
  (apply #(rf/dispatch [:elements/create {:type (key %)
                                          :attrs (val %)}]) element)
  "")

(defn circle
  "Creates a circle."
  ([cx cy r]
   (create {:circle {:cx cx :cy cy :r r}})
   "")
  
  ([[cx cy] r]
   (circle cx cy r)
   "")
  
  ([attrs]
   (create {:circle attrs})
   ""))

(defn rect
  "Creates a rectangle"
  ([x y width height]
   (create {:rect {:x x :y y :width width :height height}})
   "")

  ([[x y] [width height]]
   (rect x y width height)
   "")
  
  ([attrs]
   (create {:rect attrs})
   ""))

(defn line
  "Creates a rectangle"
  ([x1 y1 x2 y2]
   (create {:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :stroke "#000000"}})
   "")

  ([[x1 y1] [x2 y2]]
   (line x1 y1 x2 y2)
   "")

  ([attrs]
   (create {:line attrs})
   ""))

(defn set-attribute
  "Set the attribute of the selected elements."
  [name value]
  (rf/dispatch [:elements/set-attribute name value true])
  "")

(defn db
  []
  @db/app-db)

(defn document
  []
  (get-in (db) [:documents (:active-document (db))]))

(defn elements
  []
  (:elements (document)))

(defn raise
  "Raises the selected elements."
  []
  (rf/dispatch [:elements/raise])
  "")

(defn lower
  "Lowers the selected elements."
  []
  (rf/dispatch [:elements/lower])
  "")

(defn select-all
  "Selects all elements."
  []
  (rf/dispatch [:elements/select-all])
  "")

(defn deselect-all
  "Deselects all elements."
  []
  (rf/dispatch [:elements/deselect-all])
  "")

(defn to-path
  "Converts selected elements to paths."
  []
  (rf/dispatch [:elements/to-path])
  "")

(defn animate
  "Animates the selected elements."
  ([type attrs]
   (rf/dispatch [:elements/animate type attrs])
   "")
  ([attrs]
   (animate :animate attrs))
  ([]
   (animate {})))

(defn undo
  "Goes back in history."
  ([steps]
   (rf/dispatch [:history/undo steps])
   "")
  ([]
   (undo 1)))

(defn redo
  "Goes forward in history."
  ([steps]
   (rf/dispatch [:history/redo steps])
   "")
  ([]
   (redo 1)))

(defn exit
  "Closes the application."
  [element]
  (apply #(rf/dispatch [:window/close]) element)
  "")

(defn help
  "Lists available functions."
  []
  (doseq [x (keys (ns-publics 'repath.user))] (print x))
  "")

