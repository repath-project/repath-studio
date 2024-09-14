(ns renderer.reepl.subs
  (:require-macros
   [reagent.ratom :refer [reaction]]))

(defn items
  [db]
  (reaction (:items @db)))

(defn current-text
  [db]
  (let [idx (reaction (:hist-pos @db))
        history (reaction (:history @db))]
    (reaction (let [history @history
                    pos (- (count history) @idx 1)]
                {:pos pos
                 :count (count history)
                 :text (get history pos)}))))
