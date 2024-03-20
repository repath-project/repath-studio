(ns renderer.handlers
  (:require
   [re-frame.core :as rf]))

(defn set-state
  [db state]
  (assoc db :state state))

(defn set-cursor
  [db cursor]
  (assoc db :cursor cursor))

(defn set-message
  [db message]
  (assoc db :message message))

(defn add-image!
  [^js/File file adjusted-pointer-pos]
  (let [reader (js/FileReader.)]
    (.addEventListener
     reader
     "load"
     #(let [blob (.-result reader)
            img (js/document.createElement "img")]
        ;; Get the image size onload.
        (set! (.-onload img)
              (fn []
                (let [width (.-width img)
                      height (.-height img)
                      el {:type :element
                          :tag :image
                          :name (.-name file)
                          :attrs {:x (- (first adjusted-pointer-pos) (/ width 2))
                                  :y (- (second adjusted-pointer-pos) (/ height 2))
                                  :width width
                                  :height height
                                  :href blob}}]
                  (.remove img)
                  (rf/dispatch [:element/add el]))))
        (set! (.-src img) blob)))
    (.readAsDataURL reader file)))

(defn drop-files
  "https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/files"
  [{:keys [adjusted-pointer-pos] :as db} files]
  (reduce
   (fn [db file]
     (case (.-type file)
       "image/png" (add-image! file adjusted-pointer-pos)
       "image/jpeg" (add-image! file adjusted-pointer-pos)
       "image/bmp" (add-image! file adjusted-pointer-pos)
       db)) db files))

(defn drop-items
  "https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItem"
  [{:keys [adjusted-pointer-pos] :as db} items]
  (doall
   (for [item items]
     (case (.-kind item)
       "string" (.getAsString
                 item
                 #(rf/dispatch [:element/add
                                {:type :element
                                 :tag :text
                                 :content %
                                 :attrs {:x (first adjusted-pointer-pos)
                                         :y (second adjusted-pointer-pos)}}]))
       nil)))
  db)
