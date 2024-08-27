(ns renderer.document.handlers
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.document.db :as db]
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as frame.h]
   [renderer.history.handlers :as history.h]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.utils.spec :as spec]
   [renderer.utils.uuid :as uuid]
   [renderer.utils.vec :as vec]))

(defn save-format
  ([db]
   (save-format db (:active-document db)))
  ([db id]
   (let [document (-> db
                      (get-in [:documents id])
                      (select-keys [:elements :path :id])
                      (assoc :save (history.h/position db)
                             :version config/version))]

     (reduce #(update-in %1 [:elements %2] dissoc :selected?)
             document
             (keys (:elements document))))))

(defn close
  [{:keys [active-document document-tabs] :as db} id]
  (let [index (.indexOf document-tabs id)
        active-document (if (= active-document id)
                          (get document-tabs (if (zero? index)
                                               (inc index)
                                               (dec index)))
                          active-document)]
    (-> db
        (update :document-tabs #(filterv (complement #{id}) %))
        (assoc :active-document active-document)
        (update :documents dissoc id))))

(defn add-recent
  [db file-path]
  (cond-> db
    file-path
    (update :recent #(->> (conj (filterv (complement #{file-path}) %) file-path)
                          (take-last 10)
                          (vec)))))

(defn center
  "Centers the contents of the document to the current view."
  [{:keys [active-document] :as db}]
  (cond-> db
    (and active-document
         (-> db :dom-rect)
         (-> db :window :focused?)
         (not (get-in db [:documents (:active-document db) :focused?])))
    (-> (frame.h/focus-bounds :original)
        (assoc-in [:documents active-document :focused?] true))))

(defn set-active
  [db k]
  (assoc db :active-document k))

(defn search-by-path
  [{:keys [documents]} file-path]
  (some #(when (and file-path (= (:path %) file-path)) (:id %)) (vals documents)))

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
  (let [id (or (:id document) (uuid/generate-unique #(get-in db [:documents %])))
        title (or (:title document) (new-title db))
        active-index (.indexOf (:document-tabs db) (:active-document db))
        document (merge document {:id id :title title})]
    (-> db
        (assoc-in [:documents id] document)
        (assoc :active-document id)
        (update :document-tabs #(vec/add % (inc active-index) id)))))

(def default (m/decode db/document {} mt/default-value-transformer))

(defn create
  ([db]
   (create db [595 842]))
  ([db size]
   (cond-> db
     :always
     (-> (create-tab default)
         (element.h/create {:tag :canvas
                            :attrs {:fill "#eeeeee"}}))

     size
     (-> (element.h/create {:tag :svg
                            :attrs {:width (first size)
                                    :height (second size)}})
         (center))

     :always
     (history.h/finalize "Create document"))))

(defn set-global-attr
  [{active-document :active-document :as db} k v]
  (-> db
      (assoc-in [:documents active-document k] v)
      (element.h/set-attr k v)))

(defn load
  [db document]
  (let [open-id (search-by-path db (:path document))
        document (-> (merge default document)
                     (assoc :id (or open-id (uuid/generate-unique #(get-in db [:documents %])))))]
    (if (db/valid? document)
      (cond-> db
        (not open-id)
        (-> (create-tab (dissoc document :save))
            (center)
            (history.h/finalize "Load document"))

        :always
        (-> (add-recent (:path document))
            (set-active (:id document))))

      (notification.h/add
       db
       [notification.v/spec-failed "Load document" (spec/explain document db/document)]))))

(defn saved?
  [db id]
  (let [document (get-in db [:documents id])]
    (or (= (:save document)
           (get-in document [:history :position]))
        (and (not (:save document))
             (empty? (rest (get-in document [:history :states])))))))
