(ns renderer.utils.migrations
  (:require
   [clojure.set :as set]
   [renderer.utils.map :as map]))

(def key->uuid (comp uuid name))

(def migrations
  [[[0 3] (fn [document]
            (-> document
                (set/rename-keys {:id :key})
                (update :elements
                        update-vals
                        #(-> %
                             (set/rename-keys {:id :key})
                             (map/remove-nils)))))]

   [[0 4] (fn [document]
            (cond-> document
              (:id document)
              (update :id key->uuid)

              (:save document)
              (assoc :save (random-uuid))

              :always
              (-> (update :elements update-keys key->uuid)
                  (update :elements
                          update-vals
                          #(cond-> %
                             :always
                             (-> (update :id key->uuid)
                                 (update :children (fn [ks] (mapv key->uuid ks))))

                             (:parent %)
                             (update :parent key->uuid))))))]])
