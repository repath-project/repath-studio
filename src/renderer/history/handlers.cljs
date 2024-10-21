(ns renderer.history.handlers
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
   [renderer.app.effects :as-alias app.fx]
   [renderer.app.events :as-alias app.e]
   [renderer.app.handlers :as app.h]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.h]
   [renderer.history.db :refer [History HistoryState]]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.math :refer [Vec2D]]
   [renderer.utils.vec :as vec]))

(m/=> history-path [:-> App vector?])
(defn history-path
  [db]
  [:documents (:active-document db) :history])

(m/=> history [:-> App [:maybe History]])
(defn history
  [db]
  (get-in db (history-path db)))

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
  (and active-history (contains? (state active-history) :parent)))

(m/=> redos? [:-> History boolean?])
(defn redos?
  [active-history]
  (and active-history (next-position active-history)))

(m/=> swap [:-> App App])
(defn swap
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
   (let [pos (get-in db [:documents document-id :history :position])]
     (cond-> db
       pos
       (-> (update-in [:documents document-id :history :states] select-keys [pos])
           (assoc-in [:documents document-id :history :states pos :index] 0)
           (update-in [:documents document-id :history :states pos] dissoc :parent))))))

(m/=> preview [:-> App uuid? App])
(defn preview
  [db pos]
  (assoc-in db (element.h/path db) (:elements (state (history db) pos))))

(m/=> move [:-> App uuid? App])
(defn move
  [db pos]
  (-> db
      (assoc-in (conj (history-path db) :position) pos)
      (swap)))

(m/=> undo [:function
            [:-> App App]
            [:-> App pos-int? App]])
(defn undo
  ([db]
   (undo db 1))
  ([db n]
   (if (and (pos? n) (undos? (history db)))
     (recur (move db (previous-position (history db))) (dec n))
     db)))

(m/=> redo [:function
            [:-> App App]
            [:-> App pos-int? App]])
(defn redo
  ([db]
   (redo db 1))
  ([db n]
   (if (and (pos? n) (redos? (history db)))
     (recur (move db (next-position (history db))) (dec n))
     db)))

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
  (update-in db (history-path db) dissoc :states :position))

(m/=> state-count [:-> App int?])
(defn state-count
  [db]
  (-> (history db) :states count))

(m/=> set-zoom [:-> App number? App])
(defn set-zoom
  [db, zoom]
  (assoc-in db (conj (history-path db) :zoom) zoom))

(m/=> set-translate [:-> App Vec2D App])
(defn set-translate
  [db [x y]]
  (assoc-in db (conj (history-path db) :translate) [x y]))

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

(m/=> cancel [:-> App App])
(defn cancel
  [db]
  (cond-> db
    :always (-> (dissoc :drag :pointer-offset :clicked-element)
                (tool.hierarchy/activate (:tool db))
                (element.h/dissoc-temp)
                (swap))

    (= (:state db) :select)
    (element.h/clear-hovered)

    (= (:state db) :idle)
    (app.h/set-tool :transform)

    :always
    (app.h/set-state :idle)))

(m/=> update-ancestors [:-> App App])
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
        db))))

(def valid-elements? (m/validator [:map-of uuid? Element]))

(def explain-elements (m/explainer [:map-of uuid? Element]))

(m/=> finalize [:-> [:or nil? string? fn?] any?])
(defn finalize
  "Pushes changes to history."
  [explanation]
  (rf/->interceptor
   :id ::finalize
   :after (fn [context]
            (let [db (rf/get-effect context :db ::not-found)
                  elements (element.h/entities db)]
              (cond
                (or (not (or explanation (:explanation db)))
                    (= elements (:elements (state (history db)))))
                context

                (not (valid-elements? elements))
                (-> db swap (notification.h/add
                             (notification.v/spec-failed
                              explanation
                              (-> elements explain-elements me/humanize str))))

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
