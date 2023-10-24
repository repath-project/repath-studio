(ns renderer.handlers
  (:require
   [re-frame.core :as rf]
   [renderer.elements.handlers :as elements]))

(defn set-state
  [db state]
  (assoc db
         :state state
         :cursor (if (= state :clone) "copy" "default")))

(defn set-message
  [db message]
  (assoc db :message message))

(defn drop-files
  [{:keys [adjusted-mouse-pos]:as db} files]
  (reduce
   (fn [db file]
     (case (.-type file)
       "image/png"
       (elements/create db {:type :element
                            :tag :image
                            :attrs {:x (first adjusted-mouse-pos)
                                    :y (second adjusted-mouse-pos)
                                    :href (.-path file)}})
       db))
   db
   files))

(defn drop-items
  [{:keys [adjusted-mouse-pos active-document] :as db} items]
  (let [fill (get-in db [:documents active-document :fill])]
    (doall
     (for [item items]
       (case (.-kind item)
         "string"
         (.getAsString
          item
          #(rf/dispatch [:elements/create
                         {:type :element
                          :tag :text
                          :content %
                          :attrs {:x (first adjusted-mouse-pos)
                                  :y (second adjusted-mouse-pos)
                                  :fill fill}}]))

         nil))))
  db)