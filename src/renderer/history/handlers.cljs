(ns renderer.history.handlers
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [renderer.app.db :refer [App]]
   [renderer.app.effects :as-alias app.fx]
   [renderer.app.events :as-alias app.e]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.h]
   [renderer.history.db :refer [History HistoryState]]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.utils.math :refer [Vec2D]]
   [renderer.utils.vec :as vec]))

(m/=> path [:-> App [:* any?] vector?])
(defn path
  [db & more]
  (apply conj [:documents (:active-document db) :history] more))

(m/=> history [:-> App [:maybe History]])
(defn history
  [db]
  (get-in db (path db)))

(m/=> position [:-> App [:maybe uuid?]])
(defn position
  [db]
  (:position (history db)))

(m/=> state [:function
             [:-> [:maybe History] [:maybe HistoryState]]
             [:-> [:maybe History] [:maybe uuid?] [:maybe HistoryState]]])
(defn state
  ([active-history]
   (state active-history (:position active-history)))
  ([active-history current-position]
   (get-in active-history [:states current-position])))

(m/=> previous-position [:-> History [:maybe uuid?]])
(defn previous-position
  [active-history]
  (-> active-history state :parent))

(m/=> next-position [:-> History [:maybe uuid?]])
(defn next-position
  [active-history]
  (-> active-history state :children last))

(m/=> undos? [:-> History boolean?])
(defn undos?
  [active-history]
  (boolean (and active-history (:parent (state active-history)))))

(m/=> redos? [:-> History boolean?])
(defn redos?
  [active-history]
  (boolean (and active-history (next-position active-history))))

(m/=> reset-state [:-> App App])
(defn reset-state
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in (element.h/path db) (:elements (state (history db))))))

(m/=> drop-rest [:function
                 [:-> App App]
                 [:-> App uuid? App]])
(defn drop-rest
  ([db]
   (reduce drop-rest db (:document-tabs db)))
  ([db document-id]
   (let [pos (get-in db [:documents document-id :history :position])
         states-path [:documents document-id :history :states]]
     (cond-> db
       pos
       (-> (update-in states-path select-keys [pos])
           (assoc-in (conj states-path pos :index) 0)
           (update-in (conj states-path pos) dissoc :parent))))))

(m/=> preview [:-> App uuid? App])
(defn preview
  [db pos]
  (assoc-in db (element.h/path db) (-> db history (state  pos) :elements)))

(m/=> go-to [:-> App uuid? App])
(defn go-to
  [db pos]
  (-> db
      (assoc-in (path db :position) pos)
      (reset-state)))

(m/=> undo [:function
            [:-> App App]
            [:-> App pos-int? App]])
(defn undo
  ([db]
   (undo db 1))
  ([db n]
   (if-not (and (pos? n) (undos? (history db)))
     db
     (recur (go-to db (previous-position (history db))) (dec n)))))

(m/=> redo [:function
            [:-> App App]
            [:-> App pos-int? App]])
(defn redo
  ([db]
   (redo db 1))
  ([db n]
   (if-not (and (pos? n) (redos? (history db)))
     db
     (recur (go-to db (next-position (history db))) (dec n)))))

(m/=> accumulate [:-> History [:or fn? keyword?] [:vector HistoryState]])
(defn accumulate
  [active-history f]
  (loop [current-state (state active-history)
         stack []]
    (if-let [current-position (f current-state)]
      (let [accumulated-state (state active-history current-position)
            accumulated-state (dissoc accumulated-state :elements)]
        (recur accumulated-state (conj stack accumulated-state)))
      stack)))

(m/=> undos [:-> History [:vector HistoryState]])
(defn undos
  [active-history]
  (accumulate active-history :parent))

(m/=> redos [:-> History [:vector HistoryState]])
(defn redos
  [active-history]
  (accumulate active-history (fn [current-state] (-> current-state :children last))))

(m/=> clear [:-> App App])
(defn clear
  [db]
  (-> (assoc-in db (path db :states) {})
      (update-in (path db) dissoc :position)))

(m/=> state-count [:-> App int?])
(defn state-count
  [db]
  (-> (history db) :states count))

(m/=> set-zoom [:-> App number? App])
(defn set-zoom
  [db, zoom]
  (assoc-in db (path db :zoom) zoom))

(m/=> set-translate [:-> App Vec2D App])
(defn set-translate
  [db [x y]]
  (assoc-in db (path db :translate) [x y]))

(m/=> create-state [:-> App int? uuid? string? HistoryState])
(defn create-state
  [db now id explanation]
  (let [new-state {:explanation explanation
                   :elements (element.h/entities db)
                   :timestamp now
                   :index (state-count db)
                   :id id
                   :children []}]
    (cond-> new-state
      (position db)
      (assoc :parent (position db)))))

(m/=> update-ancestors [:-> App App])
(defn update-ancestors
  "Makes all ancestors of the active branch the rightmost element.
   This ensures that when users remain in the latest branch when they undo/redo."
  [db]
  (loop [node (state (history db))
         db db]
    (let [parent-id (:parent node)
          parent (state (history db) parent-id)]
      (if-not parent
        db
        (let [index (.indexOf (:children parent) (:id node))
              new-index (dec (count (:children parent)))
              children-path (path db :states parent-id :children)]
          (recur parent (update-in db children-path vec/move index new-index)))))))

(def valid-elements? (m/validator [:map-of uuid? Element]))

(def explain-elements (m/explainer [:map-of uuid? Element]))

(m/=> finalize [:-> App string? App])
(defn finalize
  "Pushes changes to history."
  [db explanation]
  (let [elements (element.h/entities db)]
    (cond
      (= elements (:elements (state (history db))))
      db

      (not (valid-elements? elements))
      (-> (reset-state db)
          (notification.h/add
           (notification.v/spec-failed
            explanation
            (-> elements explain-elements me/humanize str))))

      :else
      (let [current-position (position db)
            id (random-uuid)]
        (-> db
            (assoc-in (path db :position) id)
            (assoc-in (path db :states id)
                      (create-state db (.now js/Date) id explanation))
            (cond->
             current-position
              (-> (update-in (path db :states current-position :children) conj id)
                  (update-ancestors))))))))
