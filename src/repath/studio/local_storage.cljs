(ns repath.studio.local-storage
  (:require
   [goog.storage.Storage]
   [goog.storage.mechanism.HTML5LocalStorage]
   [cljs.reader :as reader]
   [clojure.string :as str]
   [day8.re-frame-10x.inlined-deps.re-frame.v1v1v2.re-frame.core :as rf])
  (:refer-clojure :exclude [get]))

(def storage (goog.storage.Storage. (goog.storage.mechanism.HTML5LocalStorage.)))

(def prefix "repath.studio.")

(defn- safe-key 
  "Adds a unique prefix to local storage keys to ensure they don't collide with the host application"
  [key]
  (str prefix key))

(defn get
  "Gets a value from local storage."
  ([key]
   (get key nil))
  ([key not-found]
   (let [value (.get storage (safe-key key))]
     (if (undefined? value)
       not-found
       (reader/read-string value)))))

(defn save!
  "Saves a value to local storage."
  [key value]
  (.set storage (safe-key key) (pr-str value)))

(defn delete-all!
  "Deletes all keys"
  []
  (doseq [k (js/Object.keys js/localStorage)]
    (when (str/starts-with? k prefix)
      (.remove storage k))))

(defn after
  ([key]
   (rf/after
    (fn [x]
      (save! key x))))
  ([key & ks]
   (rf/after
    (fn [x]
      (run!
       (fn [k]
         (let [v (if (vector? k) (get-in x k) (get x k))]
           (save! key v)))
       ks)))))

(rf/reg-cofx
 ::get
 (fn [coeffects {:keys [key or]}]
   (assoc coeffects
          (keyword key)
          (get key or))))