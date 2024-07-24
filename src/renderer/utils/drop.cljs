(ns renderer.utils.drop
  (:require
   [clojure.string :as str]
   [hickory.zip]
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.e]
   [renderer.utils.file :as file]))

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
                          :name (.-name file)
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
                                            :name (.-name file)
                                            :position position}]))
    (.readAsText reader file)))

(defn files!
  "https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/files"
  [position files]
  (doseq [file files]
    (case (.-type file)
      "image/png" (add-image! file position)
      "image/jpeg" (add-image! file position)
      "image/bmp" (add-image! file position)
      "image/gif" (add-image! file position)
      "image/svg+xml" (add-svg! file position)
      (when (= (last (str/split (.-name file) ".")) "rps")
        (file/read! file)))))

(defn add-text!
  [item [x y]]
  (.getAsString item #(rf/dispatch [::element.e/add
                                    {:type :element
                                     :tag :text
                                     :content %
                                     :attrs {:x x
                                             :y y}}])))

(defn items!
  "https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItem"
  [position items]
  (doseq [item items]
    (case (.-kind item)
      "string" (.getAsString item #(add-text! % position))
      nil)))
