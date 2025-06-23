(ns shared
  (:require
   [malli.core :as m]
   [renderer.document.db :refer [PersistedDocument]]))

(m/=> document->save-format [:-> PersistedDocument string?])
(defn document->save-format
  [document]
  (-> document
      (dissoc :path :id :title)
      (pr-str)))
