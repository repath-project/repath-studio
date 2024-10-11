(ns renderer.utils.drop
  (:require
   [clojure.string :as str]
   [hickory.zip]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.element.events :as-alias element.e]
   [renderer.utils.file :as file]))

(defn event-handler!
  "Gathers drop event props.
   https://developer.mozilla.org/en-US/docs/Web/API/DragEvent"
  [^js/DragEvent e]
  (.stopPropagation e)
  (.preventDefault e)

  (rf/dispatch-sync [::app.e/drag-event {:type (.-type e)
                                         :pointer-pos [(.-pageX e) (.-pageY e)]
                                         :data-transfer (.-dataTransfer e)}]))

(defn add-image!
  [^js/File file [x y]]
  (let [reader (js/FileReader.)]
    (.addEventListener
     reader
     "load"
     #(let [data-url (.-result reader)
            img (js/document.createElement "img")]
        ;; Get the image size onload.
        (set! (.-onload img)
              (fn []
                (let [width (.-width img)
                      height (.-height img)
                      el {:type :element
                          :tag :image
                          :label (.-name file)
                          :attrs {:x (- x (/ width 2))
                                  :y (- y (/ height 2))
                                  :width width
                                  :height height
                                  :href data-url}}]
                  (.remove img)
                  (rf/dispatch [::element.e/add el]))))
        (set! (.-src img) data-url)))
    (.readAsDataURL reader file)))

(defn add-svg!
  [^js/File file position]
  (let [reader (js/FileReader.)]
    (.addEventListener
     reader
     "load"
     #(rf/dispatch [::element.e/import-svg {:svg (.-result reader)
                                            :label (.-name file)
                                            :position position}]))
    (.readAsText reader file)))

(defn files!
  "https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/files"
  [position files]
  (doseq [file files]
    (when-let [file-type (.-type file)]
      (cond
        (= file-type "image/svg+xml")
        (add-svg! file position)

        (contains? #{"image/jpeg" "image/png" "image/bmp" "image/gif"} file-type)
        (add-image! file position)

        :else
        (let [extension (last (str/split (.-name file) "."))]
          (when (= extension "rps")
            (file/read! file)))))))

(defn add-text!
  [s [x y]]
  (rf/dispatch [::element.e/add
                {:type :element
                 :tag :text
                 :content s
                 :attrs {:x x
                         :y y}}]))

(defn items!
  "https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItem"
  [position items]
  (doseq [item items]
    (case (.-kind item)
      "string" (.getAsString item #(add-text! % position))
      nil)))
