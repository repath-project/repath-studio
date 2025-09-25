(ns renderer.history.handlers
  (:require
   [malli.core :as m]
   [malli.error :as m.error]
   [renderer.app.db :refer [App]]
   [renderer.db :refer [JS_Object]]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.db :refer [Explanation History HistoryState]]
   [renderer.notification.handlers :as notification.handlers]
   [renderer.notification.views :as notification.views]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.math :refer [Vec2]]
   [renderer.utils.vec :as utils.vec]))

(m/=> path [:function
            [:-> App vector?]
            [:-> App keyword? vector?]
            [:-> App keyword? [:* any?] vector?]])
(defn path
  ([db]
   [:documents (:active-document db) :history])
  ([db k]
   (conj (path db) k))
  ([db k & more]
   (apply conj (path db) k more)))

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
    (assoc-in (element.handlers/path db) (-> db history state :elements))))

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
       (update-in [:documents document-id :history :states]
                  #(-> (select-keys % [pos])
                       (assoc-in [pos :index] 0)
                       (assoc-in [pos :children] [])
                       (update pos dissoc :parent)))))))

(m/=> preview [:-> App uuid? App])
(defn preview
  [db pos]
  (let [preview-state (-> db history (state pos))
        timestamp (-> preview-state :timestamp js/Date.)]
    (-> db
        (assoc-in (document.handlers/path db :preview-label) (str timestamp))
        (assoc-in (element.handlers/path db) (:elements preview-state)))))

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

(m/=> accumulate [:-> History ifn? [:vector HistoryState]])
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
  (accumulate active-history (fn [current-state]
                               (-> current-state :children last))))

(m/=> clear [:-> App App])
(defn clear
  [db]
  (-> (assoc-in db (path db :states) {})
      (update-in (path db) dissoc :position)))

(m/=> state-count [:-> App int?])
(defn state-count
  [db]
  (-> db history :states count))

(m/=> set-zoom [:-> App number? App])
(defn set-zoom
  [db, zoom]
  (assoc-in db (path db :zoom) zoom))

(m/=> set-translate [:-> App Vec2 App])
(defn set-translate
  [db [x y]]
  (cond-> db
    (and x y)
    (assoc-in (path db :translate) [x y])))

(m/=> create-state [:-> App uuid? Explanation HistoryState])
(defn create-state
  [db id explanation]
  (let [new-state {:explanation explanation
                   :elements (get-in db (element.handlers/path db))
                   :timestamp (.now js/Date) ; REVIEW: Sideffect
                   :index (state-count db)
                   :id id
                   :children []}]
    (cond-> new-state
      (position db)
      (assoc :parent (position db)))))

(m/=> state->d3-data [:-> History uuid? [:maybe uuid?] JS_Object])
(defn state->d3-data
  [active-history id saved-history-id]
  (let [states (:states active-history)
        {:keys [index explanation children]} (get states id)
        n (count states)]
    #js {:name (apply t explanation)
         :id (str id)
         :saved (= id saved-history-id)
         :active (= id (:position active-history))
         :color (str "hsla(" (+ (* (/ 100 n) index) 20) ",40%,60%,1)")
         :children (->> children
                        (map #(state->d3-data active-history % saved-history-id))
                        (apply array))}))

(m/=> update-ancestors [:-> App App])
(defn update-ancestors
  "Makes all ancestors of the active branch the rightmost element.
   This ensures that users will stay in the latest branch when they undo/redo."
  [db]
  (loop [db db
         node (state (history db))]
    (let [parent-id (:parent node)
          parent (state (history db) parent-id)]
      (if-not parent
        db
        (let [index (.indexOf (:children parent) (:id node))
              new-index (dec (count (:children parent)))
              children-path (path db :states parent-id :children)]
          (recur (update-in db children-path utils.vec/move index new-index)
                 parent))))))

(def valid-elements? (m/validator [:map-of uuid? Element]))

(def explain-elements (m/explainer [:map-of uuid? Element]))

(m/=> finalize [:-> App Explanation App])
(defn finalize
  "Pushes changes to history."
  [db & explanation]
  (let [elements (get-in db (element.handlers/path db))]
    (cond
      (= elements (-> db history state :elements))
      db

      (not (valid-elements? elements))
      (-> (reset-state db)
          (notification.handlers/add (notification.views/spec-failed
                                      "Invalid state"
                                      (-> elements
                                          explain-elements
                                          m.error/humanize
                                          str))))

      :else
      (let [current-position (position db)
            id (random-uuid)]
        (-> db
            (assoc-in (path db :position) id)
            (assoc-in (path db :states id)
                      (create-state db id explanation))
            (cond->
             current-position
              (-> (update-in (path db :states current-position :children) conj id)
                  (update-ancestors))))))))
