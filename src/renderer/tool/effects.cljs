(ns renderer.tool.effects
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.document.events :as document.e]
   [renderer.element.events :as-alias element.e]
   [renderer.utils.dom :as dom]))

(rf/reg-fx
 ::set-pointer-capture
 (fn [pointer-id]
   (.setPointerCapture (dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::release-pointer-capture
 (fn [pointer-id]
   (.releasePointerCapture (dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::drop
 (fn [[position data-transfer]]
   (doseq [item (.-items data-transfer)]
     (case (.-kind item)
       "string"
       (let [[x y] position]
         (.getAsString item #(rf/dispatch [::element.e/add {:type :element
                                                            :tag :text
                                                            :content %
                                                            :attrs {:x x
                                                                    :y y}}])))
       nil))

   (doseq [file (.-files data-transfer)]
     (when-let [file-type (.-type file)]
       (cond
         (= file-type "image/svg+xml")
         (let [reader (js/FileReader.)]
           (.addEventListener
            reader
            "load"
            #(rf/dispatch [::element.e/import {:svg (.-result reader)
                                               :label (.-name file)
                                               :position position}]))
           (.readAsText reader file))

         (contains? #{"image/jpeg" "image/png" "image/bmp" "image/gif"} file-type)
         (rf/dispatch [::element.e/add-image file position])

         :else
         (let [extension (last (str/split (.-name file) "."))]
           (when (= extension "rps")
             (rf/dispatch [::document.e/file-read file]))))))))
