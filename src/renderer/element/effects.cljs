(ns renderer.element.effects
  (:require
   [promesa.core :as p]
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.e]
   [renderer.utils.file :as file]
   [renderer.utils.units :as units]
   [renderer.worker.events :as-alias worker.e]))

(rf/reg-fx
 ::export
 (fn [data options]
   (file/save!
    options
    (fn [^js/FileSystemFileHandle file-handle]
      (p/let [writable (.createWritable file-handle)]
        (.then (.write writable data) (.close writable)))))))

(rf/reg-fx
 ::->svg
 (fn [[elements action worker]]
   (doseq [el elements]
     (let [data-url (-> el :attrs :href)
           [x y] (:bounds el)
           canvas (js/document.createElement "canvas")
           context (.getContext canvas "2d")
           image (js/Image.)
           ;; TODO: Handle preserveAspectRatio.
           width (units/unit->px (-> el :attrs :width))
           height (units/unit->px (-> el :attrs :height))]
       (set! (.-onload image)
             #(do (set! (.-width canvas) width)
                  (set! (.-height canvas) height)
                  (.drawImage context image 0 0 width height)
                  (p/let [image-data (.getImageData context 0 0 width height)]
                    (rf/dispatch [::worker.e/create
                                  {:action action
                                   :worker worker
                                   :data {:name (:name el)
                                          :image image-data
                                          :position [x y]}
                                   :callback (fn [e]
                                               (let [data (js->clj (.. e -data) :keywordize-keys true)]
                                                 (rf/dispatch [::element.e/import-traced-image data])
                                                 (rf/dispatch [::worker.e/completed (keyword (:id data))])))}]))))
       (set! (.-src image) data-url)))))
