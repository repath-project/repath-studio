(ns repath.user
  (:require
   [re-frame.core :as rf]))

(defn move
  "Moves the selected elements."
  ([offset]
   (rf/dispatch [:elements/move offset])
   "")
  ([x y]
   (move [x y])))

(defn m
  [& args]
  (apply move args))

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
  (apply #(rf/dispatch [:elements/create {:type (key %)
                                        :attrs (val %)}]) element)
  "")