(ns renderer.dialog.handlers
  (:require
   [malli.experimental :as mx]
   [renderer.dialog.db :refer [Dialog]]))

(mx/defn create
  [db, dialog :- Dialog]
  (update db :dialogs conj dialog))
