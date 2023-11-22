(ns renderer.handlers
  (:require
   [re-frame.core :as rf]
   [renderer.element.handlers :as element-handlers]))

(defn set-state
  [db state]
  (assoc db
         :state state
         :cursor (if (= state :clone) "copy" "default")))

(defn set-message
  [db message]
  (assoc db :message message))

(defn drop-files
  "https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/files"
  [{:keys [adjusted-mouse-pos] :as db} files]
  (reduce
   (fn [db file]
     (case (.-type file)
       "image/png"
       (element-handlers/create db {:type :element
                                    :tag :image
                                    :attrs {:x (first adjusted-mouse-pos)
                                            :y (second adjusted-mouse-pos)
                                            :href (.-path file)}})

       db))
   db
   files))

(defn drop-items
  "https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItem"
  [{:keys [adjusted-mouse-pos active-document] :as db} items]
  (let [fill (get-in db [:documents active-document :fill])]
    (doall
     (for [item items]
       (case (.-kind item)
         "string" (.getAsString
                   item
                   #(rf/dispatch [:element/create
                                  {:type :element
                                   :tag :text
                                   :content %
                                   :attrs {:x (first adjusted-mouse-pos)
                                           :y (second adjusted-mouse-pos)
                                           :fill fill}}]))
         nil))))
  db)
