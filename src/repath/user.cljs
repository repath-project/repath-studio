(ns repath.user
  (:require
   [re-frame.core :as rf]))

(defn move
  "Moves the selected elements."
  [offset]
  (rf/dispatch [:elements/move offset])
  "")

(defn fill
  "Fills the selected elements."
  [color]
  (rf/dispatch [:elements/fill [color]])
  "")

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

(defn create
  "Creates a new element"
  [element]
  (rf/dispatch [:elements/create element])
  "")