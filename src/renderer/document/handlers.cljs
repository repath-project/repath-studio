(ns renderer.document.handlers
  (:require
   [renderer.utils.uuid :as uuid]
   [renderer.utils.vec :as vec]))

(defn close
  ([{:keys [active-document] :as db}]
   (close db active-document))
  ([{:keys [active-document document-tabs] :as db} key]
   (let [index (.indexOf document-tabs key)
         active-document (if (= active-document key)
                           (get document-tabs (if (zero? index)
                                                (inc index)
                                                (dec index)))
                           active-document)]
     (-> db
         (update :document-tabs #(filterv (complement #{key}) %))
         (assoc :active-document active-document)
         (update :documents dissoc key)))))

(defn expand-el
  [{:keys [active-document] :as db} el-k]
  (update-in db [:documents active-document :collapsed-keys] disj el-k))

(defn add-recent
  [db file-path]
  (update db :recent #(-> % (conj file-path) distinct)))

(defn create-tab
  [db document]
  (let [key (or (:key document) (uuid/generate))
        title (or (:title document) (str "Untitled-" (inc (count (:documents db)))))
        document-tabs (:document-tabs db)
        active-index (.indexOf document-tabs (:active-document db))
        open? (some #{key} document-tabs)
        document (merge document {:key key :title title})]
    (cond-> db
      (not open?) (update :document-tabs #(vec/add % (inc active-index) key))
      :always (-> (assoc-in [:documents key] document)
                  (assoc :active-document key)))))
