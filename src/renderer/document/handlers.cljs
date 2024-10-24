(ns renderer.document.handlers
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [malli.transform :as mt]
   [renderer.app.db :refer [App]]
   [renderer.document.db :as db :refer [Document PersistedDocument]]
   [renderer.frame.handlers :as frame.h]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.utils.vec :as vec]))

(defn path
  [db & more]
  (apply conj [:documents (:active-document db)] more))

(m/=> active [:-> App Document])
(defn active
  [db]
  (get-in db (path db)))

(m/=> persisted-format [:function
                        [:-> App PersistedDocument]
                        [:-> App uuid? PersistedDocument]])
(defn persisted-format
  ([db]
   (persisted-format db (:active-document db)))
  ([db id]
   (let [document (-> (get-in db [:documents id])
                      (assoc :version (:version db)))]
     (reduce #(update-in %1 [:elements %2] dissoc :selected)
             (m/decode PersistedDocument document mt/strip-extra-keys-transformer)
             (keys (:elements document))))))

(m/=> save-format [:-> PersistedDocument string?])
(defn save-format
  [document]
  (-> document
      (dissoc :path :id :title)
      (pr-str)))

(m/=> close [:-> App uuid? App])
(defn close
  [db id]
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

(m/=> add-recent [:-> App string? App])
(defn add-recent
  [db file-path]
  (cond-> db
    file-path
    (update :recent #(->> (conj (filterv (complement #{file-path}) %) file-path)
                          (take-last 10)
                          (vec)))))

(m/=> center [:-> App App])
(defn center
  "Centers the contents of the document to the current view."
  [db]
  (cond-> db
    (and (:active-document db)
         (-> db :dom-rect)
         (-> db :window :focused)
         (not (get-in db (path db :focused))))
    (-> (frame.h/focus-bounds :original)
        (assoc-in (path db :focused) true))))

(m/=> set-active [:-> App uuid? App])
(defn set-active
  [db id]
  (assoc db :active-document id))

(m/=> search-by-path [:-> App string? [:maybe uuid?]])
(defn search-by-path
  [db path]
  (let [documents (vals (:documents db))]
    (some #(when (and path (= (:path %) path)) (:id %)) documents)))

(m/=> new-title [:-> App string?])
(defn new-title
  [db]
  (let [documents (vals (:documents db))
        existing-titles (->> documents (map :title) set)]
    (loop [i 1]
      (let [title (str "Untitled-" i)]
        (if-not (contains? existing-titles title)
          title
          (recur (inc i)))))))

(m/=> create-tab [:-> App Document App])
(defn create-tab
  [db document]
  (let [id (:id document)
        active-index (.indexOf (:document-tabs db) (:active-document db))
        document (cond-> document
                   (not (:title document))
                   (assoc :title (new-title db)))]
    (-> db
        (assoc-in [:documents id] document)
        (assoc :active-document id)
        (update :document-tabs #(vec/add % (inc active-index) id)))))

(m/=> set-hovered-ids [:-> App [:set uuid?] App])
(defn set-hovered-ids
  [db ids]
  (assoc-in db (path db :hovered-ids) ids))

(m/=> collapse-el [:-> App uuid? App])
(defn collapse-el
  [db id]
  (update-in db (path db :collapsed-ids) conj id))

(m/=> expand-el [:-> App uuid? App])
(defn expand-el
  [db id]
  (update-in db (path db :collapsed-ids) disj id))

(m/=> attr [:-> App keyword? string?])
(defn attr
  [db k]
  (get-in (active db) [:attrs k]))

(m/=> assoc-attr [:-> App keyword? string? App])
(defn assoc-attr
  [db k v]
  (assoc-in db (path db :collapsed-ids :attrs k) v))

(m/=> load [:-> App map? App])
(defn load
  [db document]
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

        (:path document)
        (add-recent (:path document))

        :always
        (set-active (:id document)))

      (let [explanation (-> document db/explain me/humanize str)]
        (notification.h/add db (notification.v/spec-failed "Load document" explanation))))))

(m/=> saved? [:-> App uuid? boolean?])
(defn saved?
  [db id]
  (let [document (get-in db [:documents id])
        history-position (get-in document [:history :position])]
    (= (:save document) history-position)))

(m/=> saved-ids [:-> App sequential?])
(defn saved-ids
  [db]
  (filter #(saved? db %) (:document-tabs db)))
