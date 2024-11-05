(ns renderer.document.effects
  (:require
   [clojure.edn :as edn]
   [re-frame.core :as rf]
   [renderer.document.events :as-alias document.e]))

(rf/reg-fx
 ::read
 (fn [^js/File file]
   (let [reader (js/FileReader.)]
     (.addEventListener
      reader
      "load"
      #(let [document (-> (.. % -target -result)
                          (edn/read-string)
                          (assoc :title (.-name file)
                                 :path (.-path file)))]
         (rf/dispatch [::document.e/load document])))
     (.readAsText reader file))))
