(ns renderer.history.handlers
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.utils.uuid :as uuid]
   [renderer.utils.vec :as vec]))

(defn history-path [db]
  [:documents (:active-document db) :history])

(defn history
  [db]
  (get-in db (history-path db)))

(defn current-position
  [db]
  (:position (history db)))

(defn mark-restored
  [db]
  (assoc-in db (conj (history-path db) :states (current-position db) :restored?) true))

(defn state
  ([db]
   (state db (current-position db)))
  ([db position]
   (get-in (history db) [:states position])))

(defn previous-position
  [db]
  (-> db state :parent))

(defn next-position
  [db]
  (-> db state :children last))

(defn undos?
  [db]
  (boolean (previous-position db)))

(defn redos?
  [db]
  (boolean (next-position db)))

(defn swap
  ([db]
   (assoc-in db (element.h/path db) (:elements (state db)))))

(defn preview
  [db position]
  (assoc-in db (element.h/path db) (:elements (state db position))))

(defn move
  [db position]
  (-> db
      (assoc-in (conj (history-path db) :position) position)
      swap
      (update-in (conj (history-path db) :states position) dissoc :restored?)))

(defn undo
  ([db]
   (undo db 1))
  ([db n]
   (if (and (pos? n) (undos? db))
     (recur (move db (previous-position db)) (dec n))
     db)))

(defn redo
  ([db]
   (redo db 1))
  ([db n]
   (if (and (pos? n) (redos? db))
     (recur (move db (next-position db)) (dec n))
     db)))

(defn accumulate
  [db f]
  (loop [current-state (state db)
         stack []]
    (if-let [position (f current-state)]
      (let [accumulated-state (state db position)
            accumulated-state (dissoc accumulated-state :elements)]
        (recur accumulated-state (conj stack accumulated-state)))
      stack)))

(defn undos
  [db]
  (accumulate db :parent))

(defn redos
  [db]
  (accumulate db (fn [state] (-> state :children last))))

(defn state-count
  [db]
  (-> (history db) :states count))

(defn create-state
  [db id explanation]
  {:explanation explanation
   :elements (element.h/elements db)
   :timestamp (.now js/Date)
   :index (state-count db)
   :id id
   :parent (:position (history db))
   :children []})

(defn update-ancestors
  [db]
  (loop [node (state db)
         db db]
    (let [parent-id (:parent node)
          parent (state db parent-id)]
      (if parent
        (let [index (.indexOf (:children parent) (:id node))
              new-index (dec (count (:children parent)))]
          (recur parent (update-in db (conj (history-path db) :states parent-id :children) vec/move index new-index)))
        db)))) ; REVIEW

(defn finalize
  "Pushes changes to the zip-tree.
   Explicitly adding states, allows canceling actions before adding the state 
   to history. We also avoid the need of throttling in consecutive actions 
   (move, color pick etc)"
  [db explanation & more]
  (let [current-position (current-position db)
        id (uuid/generate)]
    (cond-> db
      (not= (element.h/elements db) (:elements (state db)))
      (cond->
       :always (-> (assoc-in (conj (history-path db) :position) id)
                   (assoc-in (conj (history-path db) :states id)
                             (create-state db id (apply str explanation more))))

       current-position
       (-> (update-in (conj (history-path db) :states current-position :children) conj id)
           update-ancestors)))))

(defn clear
  [db]
  (-> db
      (update-in [:documents (:active-document db)] dissoc :history)
      (finalize "Clear history")))
