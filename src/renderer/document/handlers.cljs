(ns renderer.document.handlers
  (:require
   [malli.error :as me]
   [malli.experimental :as mx]
   [renderer.document.db :as db :refer [document]]
   [renderer.element.db :as element.db :refer [attr]]
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as frame.h]
   [renderer.history.handlers :as history.h]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.utils.compatibility :as compatibility]
   [renderer.utils.vec :as vec]))

(mx/defn save-format :- document
  ([db]
   (save-format db (:active-document db)))
  ([db, id :- uuid?]
   (let [document (-> db
                      (get-in [:documents id])
                      (select-keys [:elements :path :id])
                      (assoc :save (history.h/position db)
                             :version (:version db)))]

     (reduce #(update-in %1 [:elements %2] dissoc :selected?)
             document
             (keys (:elements document))))))

(mx/defn close
  [{:keys [active-document document-tabs] :as db}, id :- uuid?]
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

(mx/defn add-recent
  [db, file-path :- string?]
  (cond-> db
    file-path
    (update :recent #(->> (conj (filterv (complement #{file-path}) %) file-path)
                          (take-last 10)
                          (vec)))))

(mx/defn center
  "Centers the contents of the document to the current view."
  [{:keys [active-document] :as db}]
  (cond-> db
    (and active-document
         (-> db :dom-rect)
         (-> db :window :focused?)
         (not (get-in db [:documents (:active-document db) :focused?])))
    (-> (frame.h/focus-bounds :original)
        (assoc-in [:documents active-document :focused?] true))))

(mx/defn set-active
  [db, id :- uuid?]
  (assoc db :active-document id))

(mx/defn search-by-path
  [{:keys [documents]}, file-path :- string?]
  (some #(when (and file-path (= (:path %) file-path)) (:id %)) (vals documents)))

(mx/defn new-title :- string?
  [{:keys [documents]}]
  (let [existing-titles (set (map :title (vals documents)))]
    (loop [i 1]
      (let [title (str "Untitled-" i)]
        (if (not (contains? existing-titles title))
          title
          (recur (inc i)))))))

(mx/defn create-tab
  [db, document :- document, id :- uuid?]
  (let [id (or (:id document) id)
        title (or (:title document) (new-title db))
        active-index (.indexOf (:document-tabs db) (:active-document db))
        document (merge document {:id id :title title})]
    (-> db
        (assoc-in [:documents id] document)
        (assoc :active-document id)
        (update :document-tabs #(vec/add % (inc active-index) id)))))

(mx/defn create-canvas
  [db, size :- [:maybe [:tuple number? number?]]]
  (cond-> db
    :always
    (element.h/create {:tag :canvas
                       :attrs {:fill "#eeeeee"}})

    size
    (-> (element.h/create {:tag :svg
                           :attrs {:width (first size)
                                   :height (second size)}})
        (center))))

(mx/defn create
  ([db, guid :- uuid?]
   (create db guid [595 842]))
  ([db, guid :- uuid?, size :- [:tuple number? number?]]
   (-> db
       (create-tab db/default guid)
       (create-canvas size))))

(mx/defn set-global-attr
  [{active-document :active-document :as db},
   k :- keyword?,
   v :- attr]
  (-> db
      (assoc-in [:documents active-document k] v)
      (element.h/set-attr k v)))

(mx/defn load
  [db, document :- document, id :- uuid?]
  (let [open-document-id (search-by-path db (:path document))
        migrated-document (compatibility/migrate-document document)
        migrated? (not= document migrated-document)
        document (-> (merge db/default migrated-document)
                     (assoc :id (or open-document-id id)))]
    (if (db/valid? document)
      (cond-> db
        (not open-document-id)
        (-> (create-tab (cond-> document (not migrated?) (dissoc :save)) id)
            (center))

        :always
        (-> (add-recent (:path document))
            (set-active (:id document))))

      (let [explanation (-> document db/explain me/humanize str)]
        (notification.h/add db [notification.v/spec-failed "Load document" explanation])))))

(mx/defn saved? :- boolean?
  [db, id :- uuid?]
  (let [document (get-in db [:documents id])]
    (boolean (or (= (:save document)
                    (get-in document [:history :position]))
                 (and (not (:save document))
                      (empty? (rest (get-in document [:history :states]))))))))
