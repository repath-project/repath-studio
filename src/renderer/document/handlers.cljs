(ns renderer.document.handlers)

(defn close
  ([{:keys [active-document] :as db}]
   (close db active-document))
  ([{:keys [active-document document-tabs] :as db} key]
  (let [index (.indexOf document-tabs key)
        active-document (if (= active-document key)
                          (get document-tabs (if (zero? index)
                                               (inc index)
                                               (dec index)))
                          active-document)]
    (-> db
        (update :document-tabs #(filterv (complement #{key}) %))
        (assoc :active-document active-document)
        (update :documents dissoc key)))))