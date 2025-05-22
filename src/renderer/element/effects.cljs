(ns renderer.element.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]
   [renderer.utils.length :as utils.length]
   [renderer.worker.events :as-alias worker.events]))

(rf/reg-fx
 ::print
 (fn [svg]
   (let [print-window (.open js/window)
         document (.-document print-window)]
     (.write document svg)
     (.print print-window)
     (.close print-window))))

(defn data-url->canvas-context!
  [data-url [w h] f]
  (let [canvas (js/document.createElement "canvas")
        context (.getContext canvas "2d")
        image (js/Image.)]
    (set! (.-onload image)
          #(do (set! (.-width canvas) w)
               (set! (.-height canvas) h)
               (.drawImage context image 0 0 w h)
               (f context)))
    (set! (.-src image) data-url)))

(rf/reg-fx
 ::trace
 (fn [images]
   (doseq [image images]
     (let [data-url (-> image :attrs :href)
           [x y] (:bbox image)
           ;; TODO: Handle preserveAspectRatio.
           w (utils.length/unit->px (-> image :attrs :width))
           h (utils.length/unit->px (-> image :attrs :height))]
       (data-url->canvas-context!
        data-url
        [w h]
        (fn [context]
          (rf/dispatch
           [::worker.events/create
            {:action "trace"
             :data {:label (:label image)
                    :image (.getImageData context 0 0 w h)
                    :position [x y]}
             :on-success [::element.events/traced]}])))))))

(rf/reg-fx
 ::import-image
 (fn [[^js/File file [x y]]]
   (let [reader (js/FileReader.)]
     (.addEventListener
      reader
      "load"
      #(let [data-url (.-result reader)
             img (js/document.createElement "img")]
         (set! (.-onload img)
               (fn []
                 (let [w (.-width img)
                       h (.-height img)]
                   (.remove img)
                   (rf/dispatch [::element.events/add
                                 {:type :element
                                  :tag :image
                                  :label (.-name file)
                                  :attrs {:x (- x (/ w 2))
                                          :y (- y (/ h 2))
                                          :width w
                                          :height h
                                          :href data-url}}]))))
         (set! (.-src img) data-url)))
     (.readAsDataURL reader file))))
