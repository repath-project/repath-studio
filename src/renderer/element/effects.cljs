(ns renderer.element.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.e]
   [renderer.utils.units :as units]
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
 ::->svg
 (fn [[elements action worker]]
   (doseq [el elements]
     (let [data-url (-> el :attrs :href)
           [x y] (:bounds el)
           ;; TODO: Handle preserveAspectRatio.
           width (units/unit->px (-> el :attrs :width))
           height (units/unit->px (-> el :attrs :height))]
       (data-url->canvas-context!
        data-url
        [width height]
        (fn [context]
          (-> (.getImageData context 0 0 width height)
              (.then (fn [image-data]
                       (rf/dispatch [::worker.e/create
                                     {:action action
                                      :worker worker
                                      :data {:name (:name el)
                                             :image image-data
                                             :position [x y]}
                                      :callback (fn [e]
                                                  (let [data (js->clj (.. e -data) :keywordize-keys true)]
                                                    (rf/dispatch [::element.e/import-traced-image data])
                                                    (rf/dispatch [::worker.e/completed (uuid (:id data))])))}]))))))))))

