(ns renderer.element.handlers
  (:require
   ["paper" :refer [Path]]
   [clojure.core.matrix :as mat]
   [clojure.set :as set]
   [clojure.string :as str]
   [hickory.core :as hickory]
   [hickory.zip]
   [malli.error :as me]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.db :as db]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.utils.attribute :as attr]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.extra :refer [partial-right]]
   [renderer.utils.hiccup :as hiccup]
   [renderer.utils.map :as map]
   [renderer.utils.path :as path]
   [renderer.utils.vec :as vec]))

(defn path
  ([db]
   [:documents (:active-document db) :elements])
  ([db id]
   (conj (path db) id)))

(defn elements
  ([db]
   (get-in db (path db)))
  ([db ids]
   (select-keys (elements db) (vec ids))))

(defn element
  [db id]
  (get (elements db) id))

(defn root
  [db]
  (some #(when (element/root? %) %) (vals (elements db))))

(defn locked?
  [db id]
  (:locked (element db id)))

(defn selected
  [db]
  (filter :selected (vals (elements db))))

(defn ratio-locked?
  [db]
  (every? element/ratio-locked? (selected db)))

(defn selected-ids
  [db]
  (set (map :id (selected db))))

(defn children-ids
  [db id]
  (:children (element db id)))

(defn parent
  ([db]
   (let [selected-ks (selected-ids db)]
     (or (parent db (first selected-ks))
         (root db))))
  ([db k]
   (when-let [parent-k (:parent (element db k))]
     (element db parent-k))))

(defn parent-container
  [db id]
  (loop [parent-el (parent db id)]
    (when parent-el
      (if (element/container? parent-el)
        parent-el
        (recur (parent db (:id parent-el)))))))

(defn adjusted-bounds
  [db id]
  (when-let [bounds (hierarchy/bounds (element db id))]
    (if-let [container (parent-container db id)]
      (let [[offset-x offset-y _ _] (hierarchy/bounds container)
            [x1 y1 x2 y2] bounds]
        [(+ x1 offset-x) (+ y1 offset-y) (+ x2 offset-x) (+ y2 offset-y)])
      bounds)))

(defn refresh-bounds
  [db id]
  (let [el (element db id)
        bounds (if (= (:tag el) :g)
                 (let [b (map #(adjusted-bounds db %) (children-ids db id))]
                   (when (seq b) (apply bounds/union b)))
                 (adjusted-bounds db id))
        assoc-bounds #(assoc % :bounds bounds)]
    (if (or (not bounds) (element/root? el))
      db
      (-> (reduce refresh-bounds db (children-ids db id))
          (update-in (conj (path db) id) assoc-bounds)))))

(defn update-el
  [db id f & more]
  (if (locked? db id)
    db
    (-> (apply update-in db (conj (path db) id) f more)
        (refresh-bounds id))))

(defn siblings-selected?
  [db]
  (let [selected-els (selected db)
        parent-els (set (map :parent selected-els))]
    (and (seq parent-els)
         (empty? (rest parent-els))
         (= (count selected-els)
            (count (children-ids db (first parent-els)))))))

(defn siblings
  ([db]
   (:children (parent db)))
  ([db id]
   (:children (parent db id))))

(defn root-children
  [db]
  (->> (children-ids db (:id (root db)))
       (mapv (elements db))))

(defn root-svgs
  [db]
  (->> db root-children (filter element/svg?)))

(defn ancestor-ids
  ([db]
   (reduce #(conj %1 (ancestor-ids db %2)) [] (selected-ids db)))
  ([db id]
   (loop [parent-id (:parent (element db id))
          parent-ids []]
     (if parent-id
       (recur
        (:parent (element db parent-id))
        (conj parent-ids parent-id))
       parent-ids))))

(defn index
  [db id]
  (when-let [sibling-els (siblings db id)]
    (.indexOf sibling-els id)))

(defn index-tree-path
  "Returns a sequence that represents the index tree path of an element.
   For example, the seventh element of the second svg on the canvas will
   return [2 7]. This is useful when we need to figure out the global index
   of nested elements."
  [db id]
  (let [ancestor-els (reverse (ancestor-ids db id))]
    (conj (mapv #(index db %) ancestor-els)
          (index db id))))

(defn descendant-ids
  ([db]
   (reduce #(set/union %1 (descendant-ids db %2)) #{} (selected-ids db)))
  ([db id]
   (loop [children-set (set (children-ids db id))
          child-keys #{}]
     (if (seq children-set)
       (recur
        (reduce #(set/union %1 (children-ids db %2)) #{} children-set)
        (set/union child-keys children-set))
       child-keys))))

(defn top-ancestor-ids
  [db]
  (set/difference (selected-ids db) (descendant-ids db)))

(defn top-selected-ancestors
  [db]
  (vals (elements db (top-ancestor-ids db))))

(defn dissoc-temp
  [db]
  (cond-> db
    (:active-document db)
    (update-in [:documents (:active-document db)] dissoc :temp-element)))

(defn assoc-temp
  [db el]
  (assoc-in db [:documents (:active-document db) :temp-element] el))

(defn get-temp
  [db]
  (get-in db [:documents (:active-document db) :temp-element]))

(defn update-prop
  [db id k & more]
  (-> (apply update-in db (conj (path db) id k) more)
      (refresh-bounds id)))

(defn assoc-prop
  ([db k v]
   (reduce (partial-right assoc-prop k v) db (selected-ids db)))
  ([db id k v]
   (-> (if (str/blank? v)
         (update-in db (conj (path db) id) dissoc k)
         (assoc-in db (conj (path db) id k) v))
       (refresh-bounds id))))

(defn dissoc-attr
  ([db k]
   (reduce (partial-right dissoc-attr k) db (selected-ids db)))
  ([db id k]
   (cond-> db
     (not (locked? db id))
     (update-prop id :attrs dissoc k))))

(defn assoc-attr
  ([db k v]
   (reduce (partial-right assoc-attr k v) db (selected-ids db)))
  ([db id k v]
   (cond-> db
     (not (locked? db id))
     (-> (assoc-in (conj (path db) id :attrs k) v)
         (refresh-bounds id)))))

(defn set-attr
  ([db k v]
   (reduce (partial-right set-attr k v) db (selected-ids db)))
  ([db id k v]
   (if (and (not (locked? db k))
            (element/supported-attr? (element db id) k))
     (if (str/blank? v)
       (dissoc-attr db id k)
       (assoc-attr db id k (str/trim (str v))))
     db)))

(defn update-attr
  [db id k f & more]
  (if (element/supported-attr? (element db id) k)
    (apply update-el db id attr.hierarchy/update-attr k f more)
    db))

(defn deselect
  ([db]
   (reduce deselect db (keys (elements db))))
  ([db id]
   (assoc-prop db id :selected false)))

(defn collapse
  ([db]
   (reduce collapse db (keys (elements db))))
  ([{:keys [active-document] :as db} id]
   (update-in db [:documents active-document :collapsed-ids] conj id)))

(defn expand
  ([db]
   (reduce expand db (keys (elements db))))
  ([{:keys [active-document] :as db} id]
   (update-in db [:documents active-document :collapsed-ids] disj id)))

(defn expand-ancestors
  [db id]
  (->> (ancestor-ids db id)
       (reduce expand db)))

(defn select
  ([db id]
   (-> db
       (expand-ancestors id)
       (assoc-prop id :selected true)))
  ([db id multiple]
   (if (element db id)
     (if multiple
       (update-prop db id :selected not)
       (-> db deselect (select id)))
     (deselect db))))

(defn select-ids
  [db ids]
  (reduce (partial-right assoc-prop :selected true) (deselect db) ids))

(defn select-all
  [db]
  (reduce select (deselect db) (if (siblings-selected? db)
                                 (children-ids db (:id (parent db (:id (parent db)))))
                                 (siblings db))))

(defn selected-tags
  [db]
  (reduce #(conj %1 (:tag %2)) #{} (selected db)))

(defn filter-by-tag
  [db tag]
  (filter #(= tag (:tag %)) (selected db)))

(defn select-same-tags
  [db]
  (let [tags (selected-tags db)]
    (reduce (fn [db {:keys [id tag]}]
              (cond-> db
                (contains? tags tag)
                (select id))) (deselect db) (vals (elements db)))))

(defn selected-sorted
  [db]
  (sort-by #(index-tree-path db (:id %)) (selected db)))

(defn top-selected-sorted
  [db]
  (sort-by #(index-tree-path db (:id %)) (top-selected-ancestors db)))

(defn selected-sorted-ids
  [db]
  (mapv :id (selected-sorted db)))

(defn top-selected-sorted-ids
  [db]
  (mapv :id (top-selected-sorted db)))

(defn invert-selection
  [db]
  (reduce (fn [db {:keys [id tag]}]
            (cond-> db
              (not (contains? #{:svg :canvas} tag))
              (update-prop id :selected not)))
          db
          (vals (elements db))))

(defn hover
  [db id]
  (update-in db [:documents (:active-document db) :hovered-ids] conj id))

(defn ignore
  [db id]
  (cond-> db
    (and (:active-document db) id)
    (update-in [:documents (:active-document db) :ignored-ids] conj id)))

(defn clear-hovered
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in [:documents (:active-document db) :hovered-ids] #{})))

(defn clear-ignored
  ([db]
   (cond-> db
     (:active-document db)
     (assoc-in [:documents (:active-document db) :ignored-ids] #{})))
  ([db id]
   (cond-> db
     (:active-document db)
     (update-in [:documents (:active-document db) :ignored-ids] disj id))))

(defn bounds
  [db]
  (element/united-bounds (selected db)))

(defn copy
  [db]
  (let [els (top-selected-sorted db)]
    (cond-> db
      (seq els)
      (assoc :copied-elements els
             :copied-bounds (bounds db)))))

(defn delete
  ([db]
   (reduce delete db (reverse (selected-sorted-ids db))))
  ([db id]
   (let [el (element db id)
         db (if (element/root? el) db (reduce delete db (:children el)))]
     (cond-> db
       (not (element/root? el))
       (-> (update-prop (:parent el) :children vec/remove-nth (index db id))
           (update-in (path db) dissoc id)
           (expand id))))))

(defn update-index
  ([db f]
   (reduce (partial-right update-index f) db (selected-sorted-ids db)))
  ([db id f]
   (let [sibling-count (count (siblings db id))
         i (index db id)
         new-index (f i sibling-count)]
     (cond-> db
       (<= 0 new-index (dec sibling-count))
       (update-prop (:id (parent db id)) :children vec/move i new-index)))))

(defn set-parent
  ([db parent-id]
   (reduce #(set-parent %1 parent-id %2) db (selected-sorted-ids db)))
  ([db parent-id id]
   (let [el (element db id)]
     (cond-> db
       (and el (not= id parent-id) (not (locked? db id)))
       (-> (update-prop (:parent el) :children #(vec (remove #{id} %)))
           (update-prop parent-id :children conj id)
           (assoc-prop id :parent parent-id))))))

(defn set-parent-at-index
  [db id parent-id i]
  (let [sibling-els (:children (element db parent-id))
        last-index (count sibling-els)]
    (-> db
        (set-parent parent-id id)
        (update-prop parent-id :children vec/move last-index i))))

(defn hovered-svg
  [db]
  (let [svgs (reverse (root-svgs db))
        pointer-pos (:adjusted-pointer-pos db)]
    (or
     (some #(when (bounds/contained-point? (:bounds %) pointer-pos) %) svgs)
     (root db))))

(defn translate
  ([db offset]
   (reduce (fn [db id]
             (let [container (parent-container db id)
                   hovered-svg-k (:id (hovered-svg db))]
               (cond-> db
                 :always
                 (translate id offset)

                 ;; REVIEW: Move this part to select tool?
                 (and (seq (selected db))
                      (empty? (rest (selected db)))
                      (contains? #{:move :clone} (:state db))
                      (not= (:id (parent db id)) hovered-svg-k)
                      (not (element/svg? (element db id))))
                 (-> (set-parent hovered-svg-k)
                     ;; FIXME: Handle nested containers.
                     (translate id (take 2 (:bounds container)))
                     (translate id (mat/mul (take 2 (:bounds (hovered-svg db))) -1))))))
           db
           (top-ancestor-ids db)))
  ([db id offset]
   (update-el db id hierarchy/translate offset)))

(defn place
  ([db pos]
   (reduce (partial-right place pos) db (top-ancestor-ids db)))
  ([db id pos]
   (update-el db id hierarchy/place pos)))

(defn scale
  [db ratio pivot-point in-place recursive]
  (let [ids-to-scale (cond-> (selected-ids db) recursive (set/union (descendant-ids db)))]
    (reduce
     (fn [db id]
       (let [pivot-point (->> (element db id) :bounds (take 2) (mat/sub pivot-point))
             db (update-el db id hierarchy/scale ratio pivot-point)]
         (if in-place
           (let [pointer-delta (mat/sub (:adjusted-pointer-pos db) (:adjusted-pointer-offset db))
                 child-ids (set (children-ids db id))
                 child-ids (set/intersection child-ids ids-to-scale)]
             (reduce (partial-right translate pointer-delta) db child-ids))
           db)))
     db
     ids-to-scale)))

(defn align
  ([db direction]
   (reduce (partial-right align direction) db (selected-ids db)))
  ([db id direction]
   (let [el-bounds (:bounds (element db id))
         center (bounds/center el-bounds)
         parent-bounds (:bounds (parent db id))
         parent-center (bounds/center parent-bounds)
         [cx cy] (mat/sub parent-center center)
         [x1 y1 x2 y2] (mat/sub parent-bounds el-bounds)]
     (translate db id (case direction
                        :top [0 y1]
                        :center-vertical [0 cy]
                        :bottom [0 y2]
                        :left [x1 0]
                        :center-horizontal [cx 0]
                        :right [x2 0])))))

(defn ->path
  ([db]
   (reduce ->path db (selected-ids db)))
  ([db id]
   (update-el db id element/->path)))

(defn stroke->path
  ([db]
   (reduce stroke->path db (selected-ids db)))
  ([db id]
   (update-el db id element/stroke->path)))

(defn overlapping-svg
  [db el-bounds]
  (let [svgs (reverse (root-svgs db))] ; Reverse to select top svgs first.
    (or
     (some #(when (bounds/contained? el-bounds (:bounds %)) %) svgs)
     (some #(when (bounds/intersect? el-bounds (:bounds %)) %) svgs)
     (root db))))

(defn create-parent-id
  [db el]
  (cond-> el
    (not (element/root? el))
    (assoc :parent (or (:parent el)
                       (:id (if (element/svg? el)
                              (root db)
                              (overlapping-svg db (hierarchy/bounds el))))))))

(defn create
  [db el]
  (let [id (random-uuid) ; REVIEW: Hard to use a coeffect because of recursion.
        new-el (->> (cond-> el (not (string? (:content el))) (dissoc :content))
                    (map/remove-nils)
                    (element/normalize-attrs)
                    (create-parent-id db))
        new-el (merge new-el db/default {:id id})
        child-els (-> (elements db (:children el)) vals (concat (:content el)))
        [x1 y1] (hierarchy/bounds (element db (:parent new-el)))
        add-children (fn [db child-els]
                       (reduce #(cond-> %1
                                  (db/tag? (:tag %2))
                                  (create (assoc %2 :parent id))) db child-els))]
    (if-not (db/valid? new-el)
      (notification.h/add db (notification.v/spec-failed
                              "Invalid element"
                              (-> new-el db/explain me/humanize str)))
      (cond-> db
        :always
        (assoc-in (conj (path db) id) new-el)

        (:parent new-el)
        (update-prop (:parent new-el) :children #(vec (conj % id)))

        (not (or (element/svg? new-el) (element/root? new-el) (:parent el)))
        (translate [(- x1) (- y1)])

        :always
        (refresh-bounds id)

        child-els
        (add-children child-els)))))

(defn create-default-canvas
  [db size]
  (cond-> db
    :always
    (create {:tag :canvas
             :attrs {:fill "#eeeeee"}})

    size
    (-> (create {:tag :svg
                 :attrs {:width (first size)
                         :height (second size)}}))))

(defn add
  ([db]
   (->> (get-temp db)
        (add db)
        (dissoc-temp)))
  ([db el]
   (create (deselect db) (assoc el :selected true))))

(defn bool
  [path-a path-b operation]
  (case operation
    :unite (.unite path-a path-b)
    :intersect (.intersect path-a path-b)
    :subtract (.subtract path-a path-b)
    :exclude (.exclude path-a path-b)
    :divide (.divide path-a path-b)))

(defn bool-operation
  [db operation]
  (let [selected-elements (top-selected-sorted db)
        attrs (-> selected-elements first element/->path :attrs)
        new-path (reduce (fn [p el]
                           (let [path-a (Path. p)
                                 path-b (-> el element/->path :attrs :d Path.)]
                             (-> (bool path-a path-b operation)
                                 (.exportSVG)
                                 (.getAttribute "d"))))
                         (:d attrs)
                         (rest selected-elements))]
    (cond-> (delete db)
      (seq new-path)
      (add {:type :element
            :tag :path
            :parent (-> selected-elements first :parent)
            :attrs (merge attrs {:d new-path})}))))

(defn paste-in-place
  ([db]
   (reduce paste-in-place (deselect db) (:copied-elements db)))
  ([db el]
   (reduce select (add db el) (selected-ids db))))

(defn paste
  ([db]
   (let [parent-el (hovered-svg db)]
     (reduce (partial-right paste parent-el) (deselect db) (:copied-elements db))))
  ([db el parent-el]
   (let [center (bounds/center (:copied-bounds db))
         el-center (bounds/center (:bounds el))
         offset (mat/sub el-center center)
         el (dissoc el :bounds)
         [s-x1 s-y1] (:bounds parent-el)
         pointer-pos (:adjusted-pointer-pos db)]
     (reduce
      select
      (cond-> db
        :always
        (-> (deselect)
            (add (assoc el :parent (:id parent-el)))
            (place (mat/add pointer-pos offset)))

        (not= (:id (root db)) (:id parent-el))
        (translate [(- s-x1) (- s-y1)])) (selected-ids db)))))

(defn duplicate-in-place
  ([db]
   (reduce duplicate-in-place (deselect db) (top-selected-sorted db)))
  ([db el]
   (create db el)))

(defn duplicate
  [db offset]
  (-> db
      (duplicate-in-place)
      (translate offset)))

(defn animate
  ([db tag attrs]
   (reduce (partial-right animate tag attrs) (deselect db) (selected db)))
  ([db el tag attrs]
   (reduce select (add db {:tag tag
                           :attrs attrs
                           :parent (:id el)}) (selected-ids db))))

(defn paste-styles
  ([db]
   (reduce paste-styles db (selected db)))
  ([{copied-elements :copied-elements :as db} el]
   ;; TODO: Merge attributes from multiple selected elements.
   (if (= 1 (count copied-elements))
     (let [attrs (:attrs (first copied-elements))
           style-attrs (disj attr/presentation :transform)]
       (reduce (fn [db attr]
                 (cond-> db
                   (attr attrs)
                   (update-attr (:id el) attr #(if % (-> attrs attr) disj))))
               db style-attrs)) db)))

(defn inherit-attrs
  [db source-el id]
  (reduce
   (fn [db attr]
     (let [source-attr (-> source-el :attrs attr)
           get-value (fn [v] (if (empty? (str v)) source-attr v))]
       (cond-> db
         source-attr
         (update-attr id attr get-value)))) db attr/presentation))

(defn group
  ([db]
   (group db (top-selected-sorted-ids db)))
  ([db ids]
   (reduce (fn [db id] (set-parent db (-> db selected-ids first) id))
           (add db {:tag :g
                    :parent (:id (parent db))}) ids)))

(defn ungroup
  ([db]
   (reduce ungroup db (selected-ids db)))
  ([db id]
   (cond-> db
     (and (not (locked? db id)) (= (:tag (element db id)) :g))
     (as-> db db
       (let [i (index db id)]
         (reduce
          (fn [db child-id]
            (-> db
                (set-parent-at-index child-id (:parent (element db id)) i)
                ;; Group attributes are inherited by its children,
                ;; so we need to maintain the presentation attrs.
                (inherit-attrs (element db id) child-id)))
          db (reverse (children-ids db id))))
       (delete db id)))))

(defn manipulate-path
  ([db action]
   (reduce (partial-right manipulate-path action) db (selected-ids db)))
  ([db id action]
   (cond-> db
     (= (:tag (element db id)) :path)
     (update-el id path/manipulate action))))

(defn import-svg
  [db {:keys [svg label position]}]
  (let [[x y] position
        hickory (hickory/as-hickory (hickory/parse svg))
        zipper (hickory.zip/hickory-zip hickory)
        svg (hiccup/find-svg zipper)
        svg (-> svg
                (assoc :label label)
                (update :attrs dissoc :desc :version :xmlns)
                (assoc-in [:attrs :x] x)
                (assoc-in [:attrs :y] y))]
    (-> (add db svg)
        (collapse))))

