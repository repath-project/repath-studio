(ns renderer.history.handlers
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [re-frame.core :as rf]
   [renderer.app.effects :as app.fx]
   [renderer.app.events :as-alias app.e]
   [renderer.element.db :as element.db]
   [renderer.element.handlers :as element.h]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.utils.vec :as vec]))

(defn history-path [db]
  [:documents (:active-document db) :history])

(defn history
  [db]
  (get-in db (history-path db)))

(defn position
  [db]
  (:position (history db)))

(defn state
  ([active-history]
   (state active-history (:position active-history)))
  ([active-history current-position]
   (get-in active-history [:states current-position])))

(defn previous-position
  [active-history]
  (-> active-history state :parent))

(defn next-position
  [active-history]
  (-> active-history state :children last))

(defn undos?
  [active-history]
  (boolean (previous-position active-history)))

(defn redos?
  [active-history]
  (boolean (next-position active-history)))

(defn swap
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in (element.h/path db) (:elements (state (history db))))))

(defn preview
  [db pos]
  (assoc-in db (element.h/path db) (:elements (state (history db) pos))))

(defn move
  [db pos]
  (-> db
      (assoc-in (conj (history-path db) :position) pos)
      (swap)))

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
  [active-history f]
  (loop [current-state (state active-history)
         stack []]
    (if-let [current-position (f current-state)]
      (let [accumulated-state (state active-history current-position)
            accumulated-state (dissoc accumulated-state :elements)]
        (recur accumulated-state (conj stack accumulated-state)))
      stack)))

(defn undos
  [active-history]
  (accumulate active-history :parent))

(defn redos
  [active-history]
  (accumulate active-history (fn [current-state] (-> current-state :children last))))

(defn clear
  [db]
  (update-in db (history-path db) dissoc :states :position))

(defn state-count
  [db]
  (-> (history db) :states count))

(defn set-zoom
  [db zoom]
  (assoc-in db (conj (history-path db) :zoom) zoom))

(defn set-translate
  [db [x y]]
  (assoc-in db (conj (history-path db) :translate) [x y]))

(defn- create-state
  [db now id explanation]
  (let [new-state {:explanation explanation
                   :elements (element.h/elements db)
                   :timestamp now
                   :index (state-count db)
                   :id id
                   :children []}]
    (cond-> new-state
      (position db)
      (assoc :parent (position db)))))

(defn- update-ancestors
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

(def valid-elements? (m/validator element.db/elements))

(defn finalize
  "Pushes changes to history."
  [explanation]
  (rf/->interceptor
   :id ::finalize
   :after (fn [context]
            (let [db (rf/get-effect context :db ::not-found)
                  elements (element.h/elements db)]
              (cond
                (or (not (or explanation (:explanation db)))
                    (= elements (:elements (state (history db)))))
                context

                (not (valid-elements? elements))
                (-> db swap (notification.h/add
                             [notification.v/spec-failed
                              explanation
                              (-> elements element.db/explain-elements me/humanize str)]))

                :else
                (let [current-position (position db)
                      id (random-uuid)
                      explanation (cond
                                    (fn? explanation) (explanation (rf/get-coeffect context :event))
                                    (string? explanation) explanation
                                    (nil? explanation) (:explanation db))
                      db (cond-> db
                           :always (-> (dissoc :explanation)
                                       (assoc-in (conj (history-path db) :position) id)
                                       (assoc-in (conj (history-path db) :states id)
                                                 (create-state db (.now js/Date) id explanation)))

                           current-position
                           (-> (update-in (conj (history-path db) :states current-position :children) conj id)
                               (update-ancestors)))]
                  (-> context
                      (rf/assoc-effect :db db)
                      (rf/assoc-effect ::app.fx/local-storage-persist db))))))))
