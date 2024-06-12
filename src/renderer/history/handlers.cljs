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
  ([history]
   (state history (:position history)))
  ([history position]
   (get-in history [:states position])))

(defn previous-position
  [history]
  (-> history state :parent))

(defn next-position
  [history]
  (-> history state :children last))

(defn undos?
  [history]
  (boolean (previous-position history)))

(defn redos?
  [history]
  (boolean (next-position history)))

(defn swap
  [db]
  (cond-> db
    (:active-document db) ; TODO: Create an interceptor to avoid this.
    (assoc-in (element.h/path db) (:elements (state (history db))))))

(defn preview
  [db position]
  (assoc-in db (element.h/path db) (:elements (state (history db) position))))

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
   (if (and (pos? n) (undos? (history db)))
     (recur (move db (previous-position (history db))) (dec n))
     db)))

(defn redo
  ([db]
   (redo db 1))
  ([db n]
   (if (and (pos? n) (redos? (history db)))
     (recur (move db (next-position (history db))) (dec n))
     db)))

(defn accumulate
  [history f]
  (loop [current-state (state history)
         stack []]
    (if-let [position (f current-state)]
      (let [accumulated-state (state history position)
            accumulated-state (dissoc accumulated-state :elements)]
        (recur accumulated-state (conj stack accumulated-state)))
      stack)))

(defn undos
  [history]
  (accumulate history :parent))

(defn redos
  [history]
  (accumulate history (fn [state] (-> state :children last))))

(defn state-count
  [db]
  (-> (history db) :states count))

(defn set-zoom
  [db zoom]
  (assoc-in db (conj (history-path db) :zoom) zoom))

(defn set-translate
  [db [x y]]
  (assoc-in db (conj (history-path db) :translate) [x y]))

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
  "Makes all ancestors of the active branch the rightmost element.
   This ensures that when users remain in the latest branch when they undo/redo."
  [db]
  (loop [node (state (history db))
         db db]
    (let [parent-id (:parent node)
          parent (state (history db) parent-id)]
      (if parent
        (let [index (.indexOf (:children parent) (:id node))
              new-index (dec (count (:children parent)))
              children-path (conj (history-path db) :states parent-id :children)]
          (recur parent (update-in db children-path vec/move index new-index)))
        db)))) ; REVIEW

(defn finalize
  "Pushes changes to history.
   Explicitly adding states, allows canceling actions before adding the state
   to history. We also avoid the need of throttling in consecutive actions
   (move, color pick etc)"
  [db explanation & more]
  (let [current-position (current-position db)
        id (uuid/generate)]
    (cond-> db
      (not= (element.h/elements db) (:elements (state (history db))))
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
      (update-in [:documents (:active-document db) :history] dissoc :states :position)
      (finalize "Clear history")))
