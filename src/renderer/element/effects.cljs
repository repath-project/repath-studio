(ns renderer.element.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias e]
   [renderer.utils.length :as length]
   [renderer.worker.events :as-alias worker.e]))

(rf/reg-fx
 ::print
 (fn [svg]
   (let [print-window (.open js/window)
         document (.-document print-window)]
     (.write document svg)
     (.print print-window)
     (.close print-window))))

(defn data-url->canvas-context!
  [data-url [width height] f]
  (let [canvas (js/document.createElement "canvas")
        context (.getContext canvas "2d")
        image (js/Image.)]
    (set! (.-onload image)
          #(do (set! (.-width canvas) width)
               (set! (.-height canvas) height)
               (.drawImage context image 0 0 width height)
               (f context)))
    (set! (.-src image) data-url)))

(rf/reg-fx
 ::trace
 (fn [images]
   (doseq [image images]
     (let [data-url (-> image :attrs :href)
           [x y] (:bounds image)
           ;; TODO: Handle preserveAspectRatio.
           width (length/unit->px (-> image :attrs :width))
           height (length/unit->px (-> image :attrs :height))]
       (data-url->canvas-context!
        data-url
        [width height]
        (fn [context]
          (rf/dispatch
           [::worker.e/create
            {:action "trace"
             :data {:label (:label image)
                    :image (.getImageData context 0 0 width height)
                    :position [x y]}
             :on-success [::e/traced]}])))))))

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
                 (let [width (.-width img)
                       height (.-height img)]
                   (.remove img)
                   (rf/dispatch [::e/add {:type :element
                                          :tag :image
                                          :label (.-name file)
                                          :attrs {:x (- x (/ width 2))
                                                  :y (- y (/ height 2))
                                                  :width width
                                                  :height height
                                                  :href data-url}}]))))
         (set! (.-src img) data-url)))
     (.readAsDataURL reader file))))
