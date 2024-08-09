(ns renderer.element.handlers
  (:require
   ["paper" :refer [Path]]
   [clojure.core.matrix :as mat]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.zip :as zip]
   [hickory.core :as hickory]
   [hickory.zip]
   [reagent.dom.server :as dom.server]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.tool.base :as tool]
   [renderer.tool.shape.path :as path]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.map :as map]
   [renderer.utils.uuid :as uuid]
   [renderer.utils.vec :as vec]
   [renderer.utils.spec :as spec]))

(defn path
  ([db]
   [:documents (:active-document db) :elements])
  ([db el-k]
   (conj (path db) el-k)))

(defn elements
  ([db]
   (get-in db (path db)))
  ([db ks]
   (select-keys (elements db) (vec ks))))

(defn element
  [db k]
  (get (elements db) k))

(defn tag
  [db k]
  (:tag (element db k)))

(defn locked?
  [db k]
  (:locked? (element db k)))

(defn selected
  [db]
  (filter :selected? (vals (elements db))))

(defn selected-keys
  [db]
  (set (map :key (selected db))))

(defn children
  [db k]
  (:children (element db k)))

(defn update-bounds
  [db k]
  (let [update #(assoc % :bounds (if (= (:tag %) :g)
                                   (tool/bounds % (elements db))
                                   (element/adjusted-bounds % (elements db))))]
    (if (= k :canvas)
      db
      (-> (reduce update-bounds db (children db k))
          (update-in (conj (path db) k) update)))))

(defn update-el
  [db k f & more]
  (if (locked? db k)
    db
    (-> (apply update-in db (conj (path db) k) f more)
        (update-bounds k))))

(defn single? [coll]
  (and (seq coll)
       (empty? (rest coll))))

(defn siblings-selected?
  [db]
  (let [selected (selected db)
        parents (set (map :parent selected))]
    (and (single? parents)
         (= (count selected)
            (count (children db (first parents)))))))

(defn parent
  ([db]
   (let [selected-ks (selected-keys db)]
     (or (parent db (if (siblings-selected? db)
                      (parent db (first selected-ks))
                      (first selected-ks)))
         (element db :canvas))))
  ([db k]
   (when-let [parent-k (:parent (element db k))]
     (element db parent-k))))

(defn siblings
  ([db]
   (:children (parent db)))
  ([db k]
   (:children (parent db k))))

(defn root-children
  [db]
  (->> (children db :canvas)
       (mapv (elements db))))

(defn root-svgs
  [db]
  (->> db
       root-children
       (filter element/svg?)))

(defn ancestor-keys
  ([db]
   (reduce #(conj %1 (ancestor-keys db %2)) [] (selected-keys db)))
  ([db k]
   (loop [parent-k (:parent (element db k))
          parent-ks []]
     (if parent-k
       (recur
        (:parent (element db parent-k))
        (conj parent-ks parent-k))
       parent-ks))))

(defn index
  [db k]
  (when-let [siblings (siblings db k)]
    (.indexOf siblings k)))

(defn index-tree-path
  "Returns a sequence that represents the index tree path of an element.
   For example, the seventh element of the second page on the canvas
   will return [2 7]. This is useful when we need to figure out the index of
   nested elements."
  [db k]
  (let [ancestors (reverse (ancestor-keys db k))]
    (conj (mapv #(index db %) ancestors)
          (index db k))))

#_(defn element-by-index
    [db i]
    (loop [element (element db :canvas)
           index 0]
      (if (= i index)
        element
        (recur (get (:children element) index) (inc index)))))

(defn descendant-keys
  ([db]
   (reduce #(set/union %1 (descendant-keys db %2)) #{} (selected-keys db)))
  ([db k]
   (loop [children-set (set (children db k))
          child-keys #{}]
     (if (seq children-set)
       (recur
        (reduce #(set/union %1 (children db %2)) #{} children-set)
        (set/union child-keys children-set))
       child-keys))))

(defn top-ancestor-keys
  [db]
  (set/difference (selected-keys db) (descendant-keys db)))

(defn top-selected-ancestors
  [db]
  (vals (elements db (top-ancestor-keys db))))

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
  [db el-k k & more]
  (-> (apply update-in db (conj (path db) el-k k) more)
      (update-bounds el-k)))

(defn toggle-prop
  [db el-k k]
  (update-prop db el-k k not))

(defn set-prop
  ([db k v]
   (reduce #(set-prop %1 %2 k v) db (selected-keys db)))
  ([db el-k k v]
   (-> (if (str/blank? v)
         (update-in db (conj (path db) el-k) dissoc k)
         (assoc-in db (conj (path db) el-k k) v))
       (update-bounds el-k))))

(defn remove-attr
  ([db k]
   (reduce #(remove-attr %1 %2 k) db (selected-keys db)))
  ([db el-k k]
   (cond-> db
     (not (locked? db el-k))
     (update-prop el-k :attrs dissoc k))))

(defn set-attr
  ([db k v]
   (reduce #(set-attr %1 %2 k v) db (selected-keys db)))
  ([db el-k k v]
   (let [attr-path (conj (path db) el-k :attrs k)]
     (if (and (not (locked? db k))
              (element/supports-attr? (element db el-k) k))
       (if (str/blank? v)
         (remove-attr db el-k k)
         (-> db
             (assoc-in attr-path (str/trim (str v)))
             (update-bounds el-k)))
       db))))

(defn update-attr
  [db el-k k f & more]
  (if (element/supports-attr? (element db el-k) k)
    (apply update-el db el-k hierarchy/update-attr k f more)
    db))

(defn deselect
  ([db]
   (reduce deselect db (keys (elements db))))
  ([db k]
   (set-prop db k :selected? false)))

(defn expand
  [{:keys [active-document] :as db} k]
  (update-in db [:documents active-document :collapsed-keys] disj k))

(defn expand-ancestors
  [db k]
  (->> (ancestor-keys db k)
       (reduce expand db)))

(defn select
  ([db k]
   (-> db
       (expand-ancestors k)
       (set-prop k :selected? true)))
  ([db k multi?]
   (if (element db k)
     (if-not multi?
       (-> db
           deselect
           (select k))
       (toggle-prop db k :selected?))
     (deselect db))))

(defn select-all
  [db]
  (reduce select (deselect db) (or (siblings db)
                                   (children db :canvas))))

(defn selected-tags
  [db]
  (reduce #(conj %1 (:tag %2)) #{} (selected db)))

(defn filter-by-tag
  [db tag]
  (filter #(= tag (:tag %)) (selected db)))

(defn select-same-tags
  [db]
  (let [selected-tags (selected-tags db)]
    (reduce (fn [db {:keys [key tag]}]
              (cond-> db
                (contains? selected-tags tag)
                (select key))) (deselect db) (vals (elements db)))))

(defn selected-sorted
  [db]
  (sort-by #(index-tree-path db (:key %)) (selected db)))

(defn top-selected-sorted
  [db]
  (sort-by #(index-tree-path db (:key %)) (top-selected-ancestors db)))

(defn selected-sorted-keys
  [db]
  (mapv :key (selected-sorted db)))

(defn top-selected-sorted-keys
  [db]
  (mapv :key (top-selected-sorted db)))

(defn invert-selection
  [db]
  (reduce (fn [db {:keys [key tag]}]
            (cond-> db
              (not (contains? #{:svg :canvas} tag))
              (update-prop key :selected? not)))
          db
          (vals (elements db))))

(defn hover
  [db k]
  (cond-> db
    k
    (update-in [:documents (:active-document db) :hovered-keys] conj k)))

(defn ignore
  [db k]
  (cond-> db
    (and (:active-document db) k)
    (update-in [:documents (:active-document db) :ignored-keys] conj k)))

(defn clear-hovered
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in  [:documents (:active-document db) :hovered-keys] #{})))

(defn clear-ignored
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in [:documents (:active-document db) :ignored-keys] #{})))

(defn lock
  ([db]
   (reduce lock db (selected-keys db)))
  ([db k]
   (set-prop db k :locked? true)))

(defn unlock
  ([db]
   (reduce unlock db (selected-keys db)))
  ([db k]
   (set-prop db k :locked? false)))

(defn bounds
  [db]
  (element/bounds (selected db)))

(defn copy
  [db]
  (let [elements (top-selected-sorted db)]
    (cond-> db
      (seq elements)
      (assoc :copied-elements elements
             :copied-bounds (bounds db)))))

(defn delete
  ([db]
   (reduce delete db (reverse (selected-sorted-keys db))))
  ([db k]
   (let [el (element db k)
         db (if (element/root? el) db (reduce delete db (:children el)))]
     (cond-> db
       (not (element/root? el))
       (-> (update-prop (:parent el) :children vec/remove-nth (index db k))
           (update-in (path db) dissoc k)
           (expand k))))))

(defn update-index
  [db k f & more]
  (let [siblings (siblings db k)
        index (index db k)
        new-index (apply f index more)]
    (cond-> db
      (<= 0 new-index (-> siblings count dec))
      (update-prop (:key (parent db k)) :children vec/move index new-index))))

(defn raise
  ([db]
   (reduce raise db (selected-sorted-keys db)))
  ([db k]
   (update-index db k inc)))

(defn lower
  ([db]
   (reduce lower db (selected-sorted-keys db)))
  ([db k]
   (update-index db k dec)))

(defn lower-to-bottom
  ([db]
   (reduce lower-to-bottom db (selected-sorted-keys db)))
  ([db k]
   (update-index db k (fn [_] 0))))

(defn raise-to-top
  ([db]
   (reduce raise-to-top db (selected-sorted-keys db)))
  ([db k]
   (update-index db k #(-> (siblings db k) count dec))))

(defn set-parent
  ([db k]
   (reduce #(set-parent %1 %2 k) db (selected-sorted-keys db)))
  ([db el-k parent-k]
   (let [el (element db el-k)]
     (cond-> db
       (and el (not= el-k parent-k) (not (locked? db parent-k)))
       (-> (update-prop (:parent el) :children #(vec (remove #{el-k} %)))
           (update-prop parent-k :children conj el-k)
           (set-prop el-k :parent parent-k))))))

(defn set-parent-at-index
  [db el-k parent-k i]
  (let [siblings (:children (element db parent-k))
        last-index (count siblings)]
    (-> db
        (set-parent el-k parent-k)
        (update-prop parent-k :children vec/move last-index i))))

(defn hovered-svg
  [db]
  (let [svgs (reverse (root-svgs db))
        pointer-pos (:adjusted-pointer-pos db)]
    (or
     (some #(when (bounds/contain-point? (:bounds %) pointer-pos) %) svgs)
     (element db :canvas))))

(defn translate
  ([db offset]
   (reduce (fn [db k]
             (let [parent-container (element/parent-container (elements db) (element db k))
                   hovered-svg-k (:key (hovered-svg db))]
               (cond-> db
                 :always
                 (translate k offset)

                 ;; REVIEW: Move this part to select tools?
                 (and (single? (selected db))
                      (contains? #{:move :clone} (:state db))
                      (not= (:key (parent db k)) hovered-svg-k)
                      (not (element/svg? (element db k))))
                 (-> (set-parent hovered-svg-k)
                     ;; FIXME: Handle nested containers.
                     (translate k (take 2 (:bounds parent-container)))
                     (translate k (mat/mul (take 2 (:bounds (hovered-svg db))) -1))))))
           db
           (top-ancestor-keys db)))
  ([db k offset]
   (update-el db k tool/translate offset)))

(defn position
  ([db pos]
   (reduce #(position %1 %2 pos) db (top-ancestor-keys db)))
  ([db k pos]
   (update-el db k tool/position pos)))

(defn scale
  ([db ratio pivot-point]
   (reduce #(scale %1 %2 ratio pivot-point) db (selected-keys db)))
  ([db k ratio pivot-point]
   (update-el db k tool/scale ratio (let [[x1 y1] (:bounds (element db k))]
                                      (mat/sub pivot-point [x1 y1])))))

(defn align
  ([db direction]
   (reduce #(align %1 %2 direction) db (selected-keys db)))
  ([db k direction]
   (let [bounds (:bounds (element db k))
         center (bounds/center bounds)
         parent-bounds (:bounds (parent db k))
         parent-center (bounds/center parent-bounds)
         [cx cy] (mat/sub parent-center center)
         [x1 y1 x2 y2] (mat/sub parent-bounds bounds)]
     (translate db k (case direction
                       :top [0 y1]
                       :center-vertical [0 cy]
                       :bottom [0 y2]
                       :left [x1 0]
                       :center-horizontal [cx 0]
                       :right [x2 0])))))

(defn ->path
  ([db]
   (reduce ->path db (selected-keys db)))
  ([db k]
   (update-el db k element/->path)))

(defn stroke->path
  ([db]
   (reduce stroke->path db (selected-keys db)))
  ([db k]
   (update-el db k element/stroke->path)))

(def default-props
  {:type :element
   :visible? true
   :locked? false
   :children []})

(defn overlapping-svg
  [db bounds]
  (let [svgs (reverse (root-svgs db))] ; Reverse to select top svgs first.
    (or
     (some #(when (bounds/contained? bounds (:bounds %)) %) svgs)
     (some #(when (bounds/intersect? bounds (:bounds %)) %) svgs)
     (element db :canvas))))

(defn create
  [db el]
  (if (element/supported? el)
    (let [key (uuid/generate)
          page (overlapping-svg db (tool/bounds el))
          parent (or (:parent el) (if (element/svg? el) :canvas (:key page)))
          children (vals (select-keys (elements db) (:children el)))
          [x1 y1] (tool/bounds (element db parent))
          children (concat children (:content el))
          new-el (merge el default-props {:key key :parent parent})
          add-children (fn [db children]
                         (reduce #(cond-> %1
                                    (element/supported? %2)
                                    (create (assoc %2 :parent key))) db children))]
      (cond-> db
        :always
        (-> (assoc-in (conj (path db) key) new-el)
            (update-prop (:parent new-el) :children #(vec (conj % key))))

        (not (or (element/svg? new-el) (:parent el)))
        (translate [(- x1) (- y1)])

        :always
        (update-bounds key)

        children
        (add-children children)))
    db))

(defn add
  ([db]
   (-> db
       (add (get-temp db))
       clear-temp))
  ([db element]
   (create (deselect db) (assoc element :selected? true))))

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
        new-path (reduce (fn [path el]
                           (let [path-a (Path. path)
                                 path-b (-> el element/->path :attrs :d Path.)]
                             (-> (bool path-a path-b operation)
                                 .exportSVG
                                 (.getAttribute "d"))))
                         (:d attrs)
                         (rest selected-elements))]
    (-> db
        delete
        (add {:type :element
              :tag :path
              :parent (-> selected-elements first :parent)
              :attrs (merge attrs {:d new-path})}))))

(defn paste-in-place
  ([db]
   (reduce paste-in-place (deselect db) (:copied-elements db)))
  ([db el]
   (reduce select (add db el) (selected-keys db))))

(defn paste
  ([db]
   (let [parent (hovered-svg db)]
     (reduce #(paste %1 %2 parent) (deselect db) (:copied-elements db))))
  ([db el parent]
   (let [center (bounds/center (:copied-bounds db))
         el-center (bounds/center (:bounds el))
         offset (mat/sub el-center center)
         el (dissoc el :bounds)
         [s-x1 s-y1] (:bounds parent)
         pointer-pos (:adjusted-pointer-pos db)]
     (reduce
      select
      (cond-> db
        :always
        (-> deselect
            (add (assoc el :parent (:key parent)))
            (position (mat/add pointer-pos offset)))

        (not= :canvas (:key parent))
        (translate [(- s-x1) (- s-y1)])) (selected-keys db)))))

(defn duplicate-in-place
  ([db]
   (reduce duplicate-in-place (deselect db) (top-selected-sorted db)))
  ([db el]
   (create db el)))

(defn duplicate
  [db offset]
  (-> db
      duplicate-in-place
      (translate offset)))

(defn animate
  ([db tag attrs]
   (reduce #(animate %1 %2 tag attrs) (deselect db) (selected db)))
  ([db el tag attrs]
   (reduce select (add db {:tag tag
                           :attrs attrs
                           :parent (:key el)}) (selected-keys db))))

(defn paste-styles
  ([db]
   (reduce paste-styles db (selected db)))
  ([{copied-elements :copied-elements :as db} el]
   ;; TODO: Merge attributes from multiple selected elements.
   (if (= 1 (count copied-elements))
     (let [attrs (:attrs (first copied-elements))
           style-attrs (disj spec/presentation-attrs :transform)]
       (reduce (fn [db attr]
                 (cond-> db
                   (attr attrs)
                   (update-attr (:key el) attr #(if % (-> attrs attr) disj))))
               db style-attrs)) db)))

(defn inherit-attrs
  [db source-el k]
  (reduce
   (fn [db attr]
     (let [source-attr (-> source-el :attrs attr)
           get-value (fn [v] (if (empty? (str v)) source-attr v))]
       (cond-> db
         source-attr
         (update-attr k attr get-value)))) db spec/presentation-attrs))

(defn group
  [db]
  (reduce (fn [db key] (set-parent db key (-> db selected-keys first)))
          (add db {:tag :g
                   :parent (:key (parent db))})
          (top-selected-sorted-keys db)))

(defn ungroup
  ([db]
   (reduce ungroup db (selected-keys db)))
  ([db k]
   (cond-> db
     (and (not (locked? db k)) (= (tag db k) :g))
     (as-> db db
       (let [i (index db k)]
         (reduce
          (fn [db el-k]
            (-> db
                (set-parent-at-index el-k (:parent (element db k)) i)
                ;; Group attributes are inherited by its children,
                ;; so we need to maintain the presentation attrs.
                (inherit-attrs (element db k) el-k)))
          db (reverse (children db k))))
       (delete db k)))))

(defn manipulate-path
  ([db action]
   (reduce #(manipulate-path %1 %2 action) db (selected-keys db)))
  ([db k action]
   (cond-> db
     (= (tag db k) :path)
     (update-el k path/manipulate action))))

(defn ->string
  [elements]
  (reduce #(-> (tool/render-to-string %2)
               dom.server/render-to-static-markup
               (str "\n" %)) "" elements))

(defn find-svg
  [zipper]
  (loop [loc zipper]
    (if (zip/end? loc)
      (zip/root loc)
      (if (= (:tag (zip/node loc)) :svg)
        (zip/node loc)
        (recur (zip/next loc))))))

(defn import-svg
  [db {:keys [svg name position]}]
  (let [[x y] position
        hickory (hickory/as-hickory (hickory/parse svg))
        zipper (hickory.zip/hickory-zip hickory)
        svg (find-svg zipper)]
    (add db (-> svg
                (assoc :name name)
                (update :attrs dissoc :desc :version :xmlns)
                (assoc-in [:attrs :x] x)
                (assoc-in [:attrs :y] y)))))
