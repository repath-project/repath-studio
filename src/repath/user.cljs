(ns repath.user
  (:require
   [re-frame.core :as rf]
   [re-frame.db :as db]))

(defn move
  "Moves the selected elements."
  ([offset]
   (rf/dispatch [:elements/move offset])
   "")
  ([x y]
   (move [x y])))

(defn fill
  "Fills the selected elements."
  [color]
  (rf/dispatch [:elements/fill color])
  "")

(defn delete
  "Deletes selected elements."
  ([element]
   (rf/dispatch [:elements/delete element]))
  ([]
   (rf/dispatch [:elements/delete])))

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
  (rf/dispatch [:elements/duplicate])
  "")

(defn create
  "Creates a new element."
  [element]
  (apply #(rf/dispatch [:elements/create {:type (key %)
                                          :attrs (val %)}]) element)
  "")

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

(defn selected
  []
  (:selected-keys (document)))

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