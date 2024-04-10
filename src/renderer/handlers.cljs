(ns renderer.handlers
  (:require
   [clojure.zip :as zip]
   [hickory.core :as hickory]
   [hickory.zip]
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
                  (rf/dispatch [:element/add el]))))
        (set! (.-src img) data-url)))
    (.readAsDataURL reader file)))

(defn import-hickory
  [hickory [x y]]
  (let [zipper (hickory.zip/hickory-zip hickory)
        svg (-> zipper zip/next zip/next zip/right zip/next)]
    (rf/dispatch [:element/add (assoc (zip/node svg)
                                      :x x
                                      :y y)])))

(defn add-svg!
  [^js/File file position]
  (let [reader (js/FileReader.)]
    (.addEventListener
     reader
     "load"
     #(let [svg-string (.-result reader)
            hickory (hickory/as-hickory (hickory/parse svg-string))]
        (import-hickory hickory position)))
    (.readAsText reader file)))

(defn drop-files
  "https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/files"
  [{:keys [adjusted-pointer-pos] :as db} files]
  (reduce
   (fn [db file]
     (case (.-type file)
       "image/png" (add-image! file adjusted-pointer-pos)
       "image/jpeg" (add-image! file adjusted-pointer-pos)
       "image/bmp" (add-image! file adjusted-pointer-pos)
       "image/svg+xml" (add-svg! file adjusted-pointer-pos)
       nil
       db)) db files))

(defn add-text!
  [item [x y]]
  (.getAsString item #(rf/dispatch [:element/add
                                    {:type :element
                                     :tag :text
                                     :content %
                                     :attrs {:x x
                                             :y y}}])))

(defn drop-items
  "https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItem"
  [{:keys [adjusted-pointer-pos] :as db} items]
  (doall
   (for [item items]
     (case (.-kind item)
       "string" (.getAsString item #(add-text! % adjusted-pointer-pos))
       nil)))
  db)
