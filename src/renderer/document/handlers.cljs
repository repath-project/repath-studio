(ns renderer.document.handlers
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.error :as m.error]
   [malli.transform :as m.transform]
   [renderer.app.db :refer [App]]
   [renderer.document.db :as document.db :refer [Document PersistedDocument SaveInfo]]
   [renderer.element.handlers :as element.handlers]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.notification.handlers :as notification.handlers]
   [renderer.notification.views :as notification.views]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.utils.math :refer [Vec2]]
   [renderer.utils.vec :as utils.vec]))

(m/=> path [:function
            [:-> App vector?]
            [:-> App keyword? vector?]
            [:-> App keyword? [:* any?] vector?]])
(defn path
  ([db]
   [:documents (:active-document db)])
  ([db k]
   (conj (path db) k))
  ([db k & more]
   (apply conj (path db) k more)))

(m/=> active [:-> App Document])
(defn active
  [db]
  (get-in db (path db)))

(m/=> set-active [:-> App uuid? App])
(defn set-active
  [db id]
  (-> (assoc db :active-document id)
      (snap.handlers/rebuild-tree)))

(m/=> persisted-format [:function
                        [:-> App PersistedDocument]
                        [:-> App uuid? PersistedDocument]])
(defn persisted-format
  ([db]
   (persisted-format db (:active-document db)))
  ([db id]
   (let [document (-> (get-in db [:documents id])
                      (assoc :version (:version db)))]
     (->> (:elements document)
          (keys)
          (reduce (fn [document el-id]
                    (update-in document [:elements el-id] dissoc :selected))
                  (m/decode PersistedDocument
                            document
                            m.transform/strip-extra-keys-transformer))))))

(m/=> close [:-> App uuid? App])
(defn close
  [db id]
  (let [{:keys [active-document document-tabs]} db
        index (.indexOf document-tabs id)
        new-index (if (zero? index) (inc index) (dec index))
        active-id (if (= active-document id)
                    (get document-tabs new-index)
                    active-document)]
    (-> (if active-id
          (set-active db active-id)
          (dissoc db :active-document))
        (update :document-tabs #(filterv (complement #{id}) %))
        (update :documents dissoc id))))

(m/=> add-recent [:-> App string? App])
(defn add-recent
  [db file-path]
  (cond-> db
    file-path
    (update :recent #(->> file-path
                          (conj (filterv (complement #{file-path}) %))
                          (take-last 10)
                          (vec)))))

(m/=> center [:-> App App])
(defn center
  [db]
  (cond-> db
    (and (:active-document db)
         (-> db :dom-rect)
         (not (get-in db (path db :centered))))
    (-> (frame.handlers/focus-bbox :original)
        (assoc-in (path db :centered) true)
        (snap.handlers/update-viewport-tree))))

(m/=> search-by-path [:-> App string? [:maybe uuid?]])
(defn search-by-path
  [db file-path]
  (->> (vals (:documents db))
       (some #(when (and file-path
                         (= (:path %) file-path))
                (:id %)))))

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
        (set-active id)
        (update :document-tabs #(utils.vec/add % (inc active-index) id)))))

(m/=> create [:function
              [:-> map? uuid? App]
              [:-> map? uuid? [:maybe Vec2] App]])
(defn create
  ([db guid]
   (create db guid [595 842]))
  ([db guid size]
   (-> (create-tab db (assoc document.db/default :id guid))
       (element.handlers/create-default-canvas size)
       (center))))

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
  (assoc-in db (path db :attrs k) v))

(m/=> saved-info [:-> Document any? SaveInfo])
(defn saved-info
  [document ^js/File file]
  {:id (:id document)
   :title (.-name file)})

(m/=> update-saved-info [:-> App SaveInfo App])
(defn update-saved-info
  [db info]
  (let [id (:id info)
        position (get-in db [:documents id :history :position])]
    (update-in db [:documents id] merge (assoc info :save position))))

(m/=> load [:-> App map? App])
(defn load
  [db document]
  (let [open-document-id (search-by-path db (:path document))
        document (merge document.db/default document)
        document (cond-> document
                   open-document-id
                   (assoc :id open-document-id))]
    (if (document.db/valid? document)
      (cond-> db
        (not open-document-id)
        (-> (create-tab document)
            (center))

        (:path document)
        (add-recent (:path document))

        :always
        (set-active (:id document)))

      (let [explanation (-> document document.db/explain m.error/humanize str)]
        (->> (notification.views/spec-failed "Load document" explanation)
             (notification.handlers/add db))))))

(m/=> saved? [:-> App uuid? boolean?])
(defn saved?
  [db id]
  (let [document (get-in db [:documents id])
        history-position (get-in document [:history :position])]
    (= (:save document) history-position)))

(m/=> saved-ids [:-> App sequential?])
(defn saved-ids
  [db]
  (filter (partial saved? db) (:document-tabs db)))

(m/=> ->save-format [:-> Document string?])
(defn ->save-format
  [document]
  (-> (apply dissoc document config/save-excluded-keys)
      (pr-str)))
