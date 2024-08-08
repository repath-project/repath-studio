(ns renderer.document.handlers
  (:require
   [renderer.document.db :as db]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h]
   [renderer.utils.uuid :as uuid]
   [renderer.utils.vec :as vec]))

(defn save-format
  [db]
  (-> db
      (get-in [:documents (:active-document db)])
      (assoc :save (history.h/current-position db))
      (dissoc :history)))

(defn close
  [{:keys [active-document document-tabs] :as db} k]
  (let [index (.indexOf document-tabs k)
        active-document (if (= active-document k)
                          (get document-tabs (if (zero? index)
                                               (inc index)
                                               (dec index)))
                          active-document)]
    (-> db
        (update :document-tabs #(filterv (complement #{k}) %))
        (assoc :active-document active-document)
        (update :documents dissoc k))))

(defn add-recent
  [db file-path]
  (cond-> db
    file-path
    (update :recent #(-> % (conj file-path) distinct vec))))

(defn set-active
  [db k]
  (assoc db :active-document k))

(defn- search-by-path
  [{:keys [documents]} file-path]
  (some #(when (and file-path (= (:path %) file-path)) (:key %)) (vals documents)))

(defn- new-title
  [{:keys [documents]}]
  (let [existing-titles (set (map :title (vals documents)))]
    (loop [i 1]
      (let [title (str "Untitled-" i)]
        (if (not (contains? existing-titles title))
          title
          (recur (inc i)))))))

(defn create-tab
  [db document]
  (let [key (or (:key document) (uuid/generate))
        title (or (:title document) (new-title db))
        active-index (.indexOf (:document-tabs db) (:active-document db))
        document (merge document {:key key :title title})]
    (-> db
        (assoc-in [:documents key] document)
        (assoc :active-document key)
        (update :document-tabs #(vec/add % (inc active-index) key)))))

(defn new
  [db]
  (-> db
      (create-tab db/default-document)
      (element.h/create {:tag :svg
                         :attrs {:width "800" :height "600"}})
      (history.h/finalize "Create document")))

(defn set-global-attr
  [{active-document :active-document :as db} k v]
  (-> db
      (assoc-in [:documents active-document k] v)
      (element.h/set-attr k v)))

(defn load
  [db document]
  (let [open-key (search-by-path db (:path document))
        document (-> document
                     (assoc :key (or open-key (uuid/generate))))]
    (cond-> db
      (not open-key)
      (-> (create-tab document)
          (history.h/finalize "Load document")
          (update-in [:documents (:key document)]
                     #(assoc % :save (-> % :history :position))))

      :always
      (-> (add-recent (:path document))
          (set-active (:key document))))))

(defn saved?
  [db k]
  (let [document (-> db :documents k)]
    (or (= (:save document)
           (get-in document [:history :position]))
        (and (not (:save document))
             (empty? (rest (get-in document [:history :states])))))))
