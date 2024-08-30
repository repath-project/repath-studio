(ns renderer.element.handlers
  (:require
   ["paper" :refer [Path]]
   [clojure.core.matrix :as mat]
   [clojure.set :as set]
   [clojure.string :as str]
   [hickory.core :as hickory]
   [hickory.zip]
   [malli.error :as me]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.element.db :as db]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.tool.base :as tool]
   [renderer.tool.shape.path :as path]
   [renderer.utils.attribute :as attr]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.hiccup :as hiccup]
   [renderer.utils.map :as map]
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

#_(defn tag
    [db k]
    (:tag (element db k)))

(defn root
  [db]
  (some #(when (element/root? %) %) (vals (elements db))))

(defn locked?
  [db id]
  (:locked? (element db id)))

(defn selected
  [db]
  (filter :selected? (vals (elements db))))

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
  (when-let [bounds (tool/bounds (element db id))]
    (if-let [container (parent-container db id)]
      (let [[offset-x offset-y _ _] (tool/bounds container)
            [x1 y1 x2 y2] bounds]
        [(+ x1 offset-x) (+ y1 offset-y)
         (+ x2 offset-x) (+ y2 offset-y)])
      bounds)))

(defn update-bounds
  [db id]
  (let [el (element db id)
        bounds (if (= (:tag el) :g)
                 (let [b (map #(adjusted-bounds db %) (children-ids db id))]
                   (when (seq b) (apply bounds/union b)))
                 (adjusted-bounds db id))
        assoc-bounds #(assoc % :bounds bounds)]
    (if (or (not bounds) (element/root? el))
      db
      (-> (reduce update-bounds db (children-ids db id))
          (update-in (conj (path db) id) assoc-bounds)))))

(defn update-el
  [db id f & more]
  (if (locked? db id)
    db
    (-> (apply update-in db (conj (path db) id) f more)
        (update-bounds id))))

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
  (->> db
       root-children
       (filter element/svg?)))

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
   For example, the seventh element of the second page on the canvas
   will return [2 7]. This is useful when we need to figure out the index of
   nested elements."
  [db id]
  (let [ancestor-els (reverse (ancestor-ids db id))]
    (conj (mapv #(index db %) ancestor-els)
          (index db id))))

#_(defn element-by-index
    [db i]
    (loop [element (root db)
           index 0]
      (if (= i index)
        element
        (recur (get (:children element) index) (inc index)))))

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

(defn clear-temp
  [db]
  (cond-> db
    (:active-document db)
    (update-in [:documents (:active-document db)] dissoc :temp-element)))

(defn set-temp
  [db el]
  (assoc-in db [:documents (:active-document db) :temp-element] el))

(defn get-temp
  [db]
  (get-in db [:documents (:active-document db) :temp-element]))

(defn update-prop
  [db id k & more]
  (-> (apply update-in db (conj (path db) id k) more)
      (update-bounds id)))

(defn toggle-prop
  [db id k]
  (update-prop db id k not))

(defn assoc-prop
  ([db k v]
   (reduce #(assoc-prop %1 %2 k v) db (selected-ids db)))
  ([db id k v]
   (-> (if (str/blank? v)
         (update-in db (conj (path db) id) dissoc k)
         (assoc-in db (conj (path db) id k) v))
       (update-bounds id))))

(defn remove-attr
  ([db k]
   (reduce #(remove-attr %1 %2 k) db (selected-ids db)))
  ([db id k]
   (cond-> db
     (not (locked? db id))
     (update-prop id :attrs dissoc k))))

(defn set-attr
  ([db k v]
   (reduce #(set-attr %1 %2 k v) db (selected-ids db)))
  ([db id k v]
   (let [attr-path (conj (path db) id :attrs k)]
     (if (and (not (locked? db k))
              (element/supported-attr? (element db id) k))
       (if (str/blank? v)
         (remove-attr db id k)
         (-> db
             (assoc-in attr-path (str/trim (str v)))
             (update-bounds id)))
       db))))

(defn update-attr
  [db id k f & more]
  (if (element/supported-attr? (element db id) k)
    (apply update-el db id hierarchy/update-attr k f more)
    db))

(defn deselect
  ([db]
   (reduce deselect db (keys (elements db))))
  ([db id]
   (assoc-prop db id :selected? false)))

(defn expand
  [{:keys [active-document] :as db} id]
  (update-in db [:documents active-document :collapsed-ids] disj id))

(defn expand-ancestors
  [db id]
  (->> (ancestor-ids db id)
       (reduce expand db)))

(defn select
  ([db id]
   (-> db
       (expand-ancestors id)
       (assoc-prop id :selected? true)))
  ([db id multi?]
   (if (element db id)
     (if-not multi?
       (-> db
           deselect
           (select id))
       (toggle-prop db id :selected?))
     (deselect db))))

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
              (update-prop id :selected? not)))
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
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in [:documents (:active-document db) :ignored-ids] #{})))

(defn lock
  ([db]
   (reduce lock db (selected-ids db)))
  ([db id]
   (assoc-prop db id :locked? true)))

(defn unlock
  ([db]
   (reduce unlock db (selected-ids db)))
  ([db id]
   (assoc-prop db id :locked? false)))

(defn bounds
  [db]
  (element/bounds (selected db)))

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
  [db id f & more]
  (let [sibling-els (siblings db id)
        i (index db id)
        new-index (apply f i more)]
    (cond-> db
      (<= 0 new-index (-> sibling-els count dec))
      (update-prop (:id (parent db id)) :children vec/move i new-index))))

(defn raise
  ([db]
   (reduce raise db (selected-sorted-ids db)))
  ([db id]
   (update-index db id inc)))

(defn lower
  ([db]
   (reduce lower db (selected-sorted-ids db)))
  ([db id]
   (update-index db id dec)))

(defn lower-to-bottom
  ([db]
   (reduce lower-to-bottom db (selected-sorted-ids db)))
  ([db id]
   (update-index db id (fn [_] 0))))

(defn raise-to-top
  ([db]
   (reduce raise-to-top db (selected-sorted-ids db)))
  ([db id]
   (update-index db id #(-> (siblings db id) count dec))))

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
     (some #(when (bounds/contain-point? (:bounds %) pointer-pos) %) svgs)
     (root db))))

(defn translate
  ([db offset]
   (reduce (fn [db id]
             (let [container (parent-container db id)
                   hovered-svg-k (:id (hovered-svg db))]
               (cond-> db
                 :always
                 (translate id offset)

                 ;; REVIEW: Move this part to select tools?
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
   (update-el db id tool/translate offset)))

(defn position
  ([db pos]
   (reduce #(position %1 %2 pos) db (top-ancestor-ids db)))
  ([db id pos]
   (update-el db id tool/position pos)))

(defn scale
  [db ratio pivot-point recur?]
  (reduce
   (fn [db el]
     (let [pivot-point (->> (element db el) :bounds (take 2) (mat/sub pivot-point))]
       (update-el db el tool/scale ratio pivot-point)))
   db
   (cond-> (selected-ids db)
     recur? (concat (descendant-ids db)))))

(defn align
  ([db direction]
   (reduce #(align %1 %2 direction) db (selected-ids db)))
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
                              (overlapping-svg db (tool/bounds el))))))))

(defn create
  [db el]
  (let [id (random-uuid)
        new-el (create-parent-id db el)
        child-els (vals (select-keys (elements db) (:children el)))
        [x1 y1] (tool/bounds (element db (:parent new-el)))
        child-els (concat child-els (:content el))
        defaults db/default
        new-el (merge new-el defaults {:id id})
        new-el (cond-> new-el (not (string? (:content new-el))) (dissoc :content))
        add-children (fn [db child-els]
                       (reduce #(cond-> %1
                                  (db/tag? (:tag %2))
                                  (create (assoc %2 :parent id))) db child-els))
        new-el (map/remove-nils new-el)]
    (if-not (db/valid? new-el)
      (notification.h/add db [notification.v/spec-failed
                              "Invalid element"
                              (-> new-el db/explain me/humanize str)])

      (cond-> db
        :always
        (assoc-in (conj (path db) id) new-el)

        (:parent new-el)
        (update-prop (:parent new-el) :children #(vec (conj % id)))

        (not (or (element/svg? new-el) (element/root? new-el) (:parent el)))
        (translate [(- x1) (- y1)])

        :always
        (update-bounds id)

        child-els
        (add-children child-els)))))

(defn add
  ([db]
   (-> db
       (add (get-temp db))
       (clear-temp)))
  ([db el]
   (create (deselect db) (assoc el :selected? true))))

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
    (-> db
        (delete)
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
     (reduce #(paste %1 %2 parent-el) (deselect db) (:copied-elements db))))
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
            (position (mat/add pointer-pos offset)))

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
   (reduce #(animate %1 %2 tag attrs) (deselect db) (selected db)))
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
  [db]
  (->> (top-selected-sorted-ids db)
       (reduce (fn [db id] (set-parent db (-> db selected-ids first) id))
               (add db {:tag :g
                        :parent (:id (parent db))}))))

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
   (reduce #(manipulate-path %1 %2 action) db (selected-ids db)))
  ([db id action]
   (cond-> db
     (= (:tag (element db id)) :path)
     (update-el id path/manipulate action))))

(defn import-svg
  [db {:keys [svg label pos]}]
  (let [[x y] pos
        hickory (hickory/as-hickory (hickory/parse svg))
        zipper (hickory.zip/hickory-zip hickory)
        svg (hiccup/find-svg zipper)]
    (add db (-> svg
                (assoc :label label)
                (update :attrs dissoc :desc :version :xmlns)
                (assoc-in [:attrs :x] x)
                (assoc-in [:attrs :y] y)))))

