(ns renderer.document.handlers
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [malli.experimental :as mx]
   [malli.transform :as mt]
   [renderer.document.db :as db :refer [Document PersistedDocument]]
   [renderer.frame.handlers :as frame.h]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.utils.vec :as vec]))

(mx/defn active
  [db]
  (get-in db [:documents (:active-document db)]))

(mx/defn persisted-format :- PersistedDocument
  ([db]
   (persisted-format db (:active-document db)))
  ([db, id :- uuid?]
   (let [document (-> (get-in db [:documents id])
                      (assoc :version (:version db)))]
     (reduce #(update-in %1 [:elements %2] dissoc :selected)
             (m/decode PersistedDocument document mt/strip-extra-keys-transformer)
             (keys (:elements document))))))

(mx/defn save-format :- string?
  [document :- PersistedDocument]
  (-> document
      (dissoc :path :id :title)
      (pr-str)))

(mx/defn close
  [db, id :- uuid?]
  (let [{:keys [active-document document-tabs]} db
        index (.indexOf document-tabs id)
        active-document (if (= active-document id)
                          (get document-tabs (if (zero? index)
                                               (inc index)
                                               (dec index)))
                          active-document)]
    (-> db
        (update :document-tabs #(filterv (complement #{id}) %))
        (assoc :active-document active-document)
        (update :documents dissoc id))))

(mx/defn add-recent
  [db, file-path :- string?]
  (cond-> db
    file-path
    (update :recent #(->> (conj (filterv (complement #{file-path}) %) file-path)
                          (take-last 10)
                          (vec)))))

(mx/defn center
  "Centers the contents of the document to the current view."
  [db]
  (cond-> db
    (and (:active-document db)
         (-> db :dom-rect)
         (-> db :window :focused)
         (not (get-in db [:documents (:active-document db) :focused])))
    (-> (frame.h/focus-bounds :original)
        (assoc-in [:documents (:active-document db) :focused] true))))

(mx/defn set-active
  [db, id :- uuid?]
  (assoc db :active-document id))

(mx/defn search-by-path
  [db, path :- string?]
  (let [documents (vals (:documents db))]
    (some #(when (and path (= (:path %) path)) (:id %)) documents)))

(mx/defn new-title :- string?
  [db]
  (let [documents (vals (:documents db))
        existing-titles (->> documents (map :title) set)]
    (loop [i 1]
      (let [title (str "Untitled-" i)]
        (if-not (contains? existing-titles title)
          title
          (recur (inc i)))))))

(mx/defn create-tab
  [db, document :- Document]
  (let [id (:id document)
        active-index (.indexOf (:document-tabs db) (:active-document db))
        document (cond-> document
                   (not (:title document))
                   (assoc :title (new-title db)))]
    (-> db
        (assoc-in [:documents id] document)
        (assoc :active-document id)
        (update :document-tabs #(vec/add % (inc active-index) id)))))

(mx/defn attr
  [db, k :- keyword?]
  (get-in (active db) [:attrs k]))

(mx/defn assoc-attr
  [db, k :- keyword?, v :- any?]
  (assoc-in db [:documents (:active-document db) :attrs k] v))

(mx/defn update-attr
  ([db, k :- keyword?, f]
   (update-in db [:documents (:active-document db) :attrs k] f))
  ([db, k :- keyword?, f arg1]
   (update-in db [:documents (:active-document db) :attrs k] f arg1))
  ([db, k :- keyword?, f arg1 arg2]
   (update-in db [:documents (:active-document db) :attrs k] f arg1 arg2))
  ([db, k :- keyword?, f arg1 arg2 arg3]
   (update-in db [:documents (:active-document db) :attrs k] f arg1 arg2 arg3))
  ([db, k :- keyword?, f arg1 arg2 arg3 & more]
   (apply update-in db [:documents (:active-document db) :attrs k] f arg1 arg2 arg3 more)))

(mx/defn load
  [db, document]
  (let [open-document-id (search-by-path db (:path document))
        document (merge db/default document)
        document (cond-> document
                   open-document-id
                   (assoc :id open-document-id))]
    (if (db/valid? document)
      (cond-> db
        (not open-document-id)
        (-> (create-tab document)
            (center))

        :always
        (-> (add-recent (:path document))
            (set-active (:id document))))

      (let [explanation (-> document db/explain me/humanize str)]
        (notification.h/add db (notification.v/spec-failed "Load document" explanation))))))

(mx/defn saved? :- boolean?
  [db, id :- uuid?]
  (let [document (get-in db [:documents id])
        history-position (get-in document [:history :position])]
    (= (:save document) history-position)))

(mx/defn saved-ids :- sequential?
  [db]
  (filter #(saved? db %) (:document-tabs db)))
