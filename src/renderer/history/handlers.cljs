(ns renderer.history.handlers
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [malli.experimental :as mx]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.app.events :as-alias app.e]
   [renderer.app.handlers :as app.h]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.h]
   [renderer.history.db :refer [HistoryState]]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.math :refer [Vec2D]]
   [renderer.utils.vec :as vec]))

(defn history-path
  [db]
  [:documents (:active-document db) :history])

(defn history
  [db]
  (get-in db (history-path db)))

(mx/defn position :- [:maybe uuid?]
  [db]
  (:position (history db)))

(mx/defn state :- [:maybe HistoryState]
  ([active-history]
   (state active-history (:position active-history)))
  ([active-history, current-position :- [:maybe uuid?]]
   (get-in active-history [:states current-position])))

(mx/defn previous-position :- [:maybe uuid?]
  [active-history]
  (-> active-history state :parent))

(mx/defn next-position :- [:maybe uuid?]
  [active-history]
  (-> active-history state :children last))

(defn undos?
  [active-history]
  (contains? (state active-history) :parent))

(defn redos?
  [active-history]
  (boolean (next-position active-history)))

(defn swap
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in (element.h/path db) (:elements (state (history db))))))

(mx/defn drop-rest
  ([db]
   (reduce drop-rest db (:document-tabs db)))
  ([db, document-id :- uuid?]
   (let [pos (get-in db [:documents document-id :history :position])]
     (cond-> db
       pos
       (-> (update-in [:documents document-id :history :states] select-keys [pos])
           (assoc-in [:documents document-id :history :states pos :index] 0)
           (update-in [:documents document-id :history :states pos] dissoc :parent))))))

(mx/defn preview
  [db, pos :- uuid?]
  (assoc-in db (element.h/path db) (:elements (state (history db) pos))))

(mx/defn move
  [db, pos :- uuid?]
  (-> db
      (assoc-in (conj (history-path db) :position) pos)
      (swap)))

(mx/defn undo
  ([db]
   (undo db 1))
  ([db, n :- int?]
   (if (and (pos? n) (undos? (history db)))
     (recur (move db (previous-position (history db))) (dec n))
     db)))

(mx/defn redo
  ([db]
   (redo db 1))
  ([db, n :- int?]
   (if (and (pos? n) (redos? (history db)))
     (recur (move db (next-position (history db))) (dec n))
     db)))

(mx/defn accumulate
  [active-history, f :- [:or fn? keyword?]]
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

(mx/defn set-zoom
  [db, zoom :- number?]
  (assoc-in db (conj (history-path db) :zoom) zoom))

(mx/defn set-translate
  [db, [x y] :- Vec2D]
  (assoc-in db (conj (history-path db) :translate) [x y]))

(mx/defn create-state :- HistoryState
  [db, now :- int?, id :- uuid?, explanation :- string?]
  (let [new-state {:explanation explanation
                   :elements (element.h/elements db)
                   :timestamp now
                   :index (state-count db)
                   :id id
                   :children []}]
    (cond-> new-state
      (position db)
      (assoc :parent (position db)))))

(defn cancel
  [db]
  (cond-> db
    :always (-> (dissoc :drag :pointer-offset :clicked-element)
                (tool.hierarchy/activate (:tool db))
                (element.h/dissoc-temp)
                (swap))

    (= (:state db) :select)
    (element.h/clear-hovered)

    (= (:state db) :default)
    (app.h/set-tool :select)

    :always
    (app.h/set-state :default)))

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
        db))))

(def valid-elements? (m/validator [:map-of uuid? Element]))

(def explain-elements (m/explainer [:map-of uuid? Element]))

(mx/defn finalize
  "Pushes changes to history."
  [explanation :- [:or nil? string? fn?]]
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
                              (-> elements explain-elements me/humanize str)]))

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
                      (rf/assoc-effect ::app.fx/persist db))))))))
