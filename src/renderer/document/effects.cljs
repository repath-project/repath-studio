(ns renderer.document.effects
  (:require
   [promesa.core :as p]
   [re-frame.core :as rf]
   [renderer.document.events :as-alias document.e]
   [renderer.utils.file :as file]))

(def file-picker-options
  {:startIn "documents"
   :types [{:accept {"application/repath-studio" [".rps"]}}]})

(rf/reg-fx
 ::open
 (fn []
   (file/open! file-picker-options)))

(rf/reg-fx
 ::download
 (fn [data]
   (file/download! data)))

(rf/reg-fx
 ::save-as
 (fn [data]
   (file/save!
    file-picker-options
    (fn [^js/FileSystemFileHandle file-handle]
      (p/let [writable (.createWritable file-handle)]
        (.then (.write writable (pr-str (dissoc data :path)))
               (let [info (assoc data :title (.-name file-handle))]
                 (.close writable)
                 (rf/dispatch [::document.e/saved (select-keys info [:id :title :save])]))))))))
