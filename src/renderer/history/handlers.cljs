(ns renderer.history.handlers
  (:require
   [clojure.zip :as zip]
   [renderer.element.handlers :as element.h]))

(defn history-path [db]
  [:documents (:active-document db) :history])

(defn history
  [db]
  (get-in db (history-path db)))

(defn step-count
  [db]
  (if (history db)
    (count (flatten (zip/root (history db))))
    0))

(defn state
  [db explanation]
  (with-meta (element.h/elements db) {:explanation explanation
                                     :date (.now js/Date)
                                     :index (step-count db)}))

(defn init
  "Creates the vector zipper and moves down to the first position."
  [db explanation]
  (assoc-in db (history-path db) (zip/down (zip/vector-zip [(state db explanation)]))))

(defn undos?
  "Checks if there are available redos.
   We need to move back twice to avoid wiping the initial elements."
  [history]
  (and history (-> history
                   zip/prev
                   zip/prev)))

(defn redos?
  [history]
  (and history (not (zip/end? (zip/next history)))))

(defn swap
  [db]
  (assoc-in db (element.h/path db) (zip/node (history db))))

(defn move
  [db f]
  (-> db
      (update-in (history-path db) f)
      swap))

(defn undo
  ([db]
   (undo db 1))
  ([db n]
   (if (and (pos? n) (undos? (history db)))
     (recur (move db zip/prev) (dec n))
     db)))

(defn redo
  ([db]
   (redo db 1))
  ([db n]
   (if (and (pos? n) (redos? (history db)))
     (recur (move db zip/next) (dec n))
     db)))

(defn accumulate
  [history f]
  (loop [history history
         stack []]
    (if (and history (not (zip/end? history)))
      (if (zip/branch? history)
        (recur (f history) stack)
        (recur (f history) (conj stack (meta (zip/node history)))))
      stack)))

(defn undos
  [history]
  (accumulate history zip/prev))

(defn redos
  [history]
  (accumulate history zip/next))

(print )

(defn finalize
  "Pushes changes to the zip-tree.
   Explicitly adding states, allows canceling actions before adding the state 
   to history. We also avoid the need of throttling in consecutive actions 
   (move, color pick etc)"
  [db explanation & more]
  (let [explanation (apply str explanation more)
        state (state db explanation)
        history (history db)]
    (assoc-in db
              (history-path db)
              (cond-> history
                (redos? history) (zip/replace (conj (conj (zip/rights history) (zip/node history)) state))
                (zip/branch? history) zip/down
                (not (redos? history)) (zip/insert-right state)
                :always zip/rightmost))))
