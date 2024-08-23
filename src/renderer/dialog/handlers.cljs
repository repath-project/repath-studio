(ns renderer.dialog.handlers)

(defn create
  [db dialog]
  (update db :dialogs conj dialog))
