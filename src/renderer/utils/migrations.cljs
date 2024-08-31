(ns renderer.utils.migrations
  (:require
   [clojure.set :as set]
   [renderer.utils.map :as map]))

(def key->uuid (comp uuid name))

(def migrations
  [[[0 3 0] (fn [document]
              (-> document
                  (set/rename-keys {:key :id})
                  (update :elements
                          update-vals
                          #(-> %
                               (set/rename-keys {:key :id})
                               (map/remove-nils)))))]

   [[0 4 0] (fn [document]
              (cond-> document
                (:id document)
                (update :id key->uuid)

                (:save document)
                (update :save key->uuid)

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
