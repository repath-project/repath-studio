(ns renderer.element.handlers
  (:require
   ["paper" :refer [Path]]
   [clojure.core.matrix :as mat]
   [clojure.set :as set]
   [clojure.string :as str]
   [reagent.dom.server :as dom.server]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.document.handlers :as document.h]
   [renderer.tools.base :as tools]
   [renderer.tools.shape.path :as path]
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
  ([db keys]
   ((apply juxt keys) (elements db))))

(defn element
  [db k]
  (get (elements db) k))

(defn update-bounds
  [db el-k]
  (as-> db db
    (update-in db (conj (path db) el-k) #(assoc % :bounds (element/adjusted-bounds % (elements db))))
    (reduce #(update-bounds %1 %2) db (:children (element db el-k)))))

(defn update-el
  [db el f & more]
  (-> (apply update-in db (conj (path db) (:key el)) f more)
      (update-bounds (:key el))))

(defn selected
  [db]
  (filter :selected? (vals (elements db))))

(defn selected-keys
  [db]
  (set (map :key (selected db))))

(defn single? [coll]
  (and (seq coll)
       (empty? (rest coll))))

(defn siblings-selected?
  [db]
  (let [selected (selected db)
        parents (set (map :parent selected))]
    (and (single? parents)
         (= (count selected)
            (count (:children (element db (first parents))))))))

(defn parent
  ([db]
   (let [selected (selected db)]
     (or (cond
           (siblings-selected? db)
           (parent db (parent db (first selected)))

           (single? selected)
           (parent db (first selected)))
         (element db :canvas))))
  ([db el]
   (when-let [parent (:parent el)]
     (element db parent))))

(defn siblings
  ([db]
   (:children (parent db)))
  ([db el]
   (:children (parent db el))))

(defn root-children
  [db]
  (->> (element db :canvas)
       :children
       (mapv (elements db))))

(defn root-svgs
  [db]
  (->> db
       root-children
       (filter element/svg?)))

(defn ancestor-keys
  ([db]
   (reduce #(set/union %1 (ancestor-keys db %2)) #{} (selected db)))
  ([db el]
   (loop [parent-key (:parent el)
          parent-keys #{}]
     (if parent-key
       (recur
        (:parent (element db parent-key))
        (conj parent-keys parent-key))
       parent-keys))))

(defn index
  [db el]
  (when-let [siblings (siblings db el)]
    (.indexOf siblings (:key el))))

(defn index-tree-path
  "Returns a sequence that represents the index tree path of an element.
   For example, the seventh element of the second page on the canvas
   will return [2 7]. This is useful when we need to figure out the index of 
   nested elements."
  [db el]
  (let [ancestors (-> db (ancestor-keys el) reverse)]
    (conj (mapv #(index db %) (elements db ancestors))
          (index db el))))

#_(defn element-by-index
    [db index-vec]
    (loop [element (element db :canvas)
           i 0]
      (if (= (count index-vec) i)
        element
        (recur (get (:children element) index) (inc i)))))

(defn descendant-keys
  ([db]
   (reduce #(set/union %1 (descendant-keys db %2)) #{} (selected db)))
  ([db el]
   (loop [children (set (:children el))
          child-keys #{}]
     (if (seq children)
       (recur
        (reduce #(set/union %1 (:children (element db %2))) #{} children)
        (set/union child-keys children))
       child-keys))))

(defn top-ancestor-keys
  [db]
  (set/difference (selected-keys db) (descendant-keys db)))

(defn top-selected-ancestors
  [db]
  (elements db (top-ancestor-keys db)))

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
     (not (:locked? (element db el-k)))
     (update-prop el-k :attrs dissoc k))))

(defn supports-attr?
  [el k]
  (-> el element/attributes k))

(defn set-attr
  ([db k v]
   (reduce #(set-attr %1 %2 k v) db (selected-keys db)))
  ([db el-k k v]
   (let [attr-path (conj (path db) el-k :attrs k)
         el (element db el-k)]
     (if (and (not (:locked? el)) (supports-attr? el k))
       (if (str/blank? v)
         (remove-attr db el-k k)
         (-> db
             (assoc-in attr-path (str/trim (str v)))
             (update-bounds el-k)))
       db))))

(defn update-attr
  [db el k f & more]
  (if (and (not (:locked? el)) (supports-attr? el k))
    (apply update-el db el hierarchy/update-attr k f more)
    db))

(defn deselect
  ([db]
   (reduce deselect db (keys (elements db))))
  ([db key]
   (set-prop db key :selected? false)))

(defn expand-ancestors
  [db k]
  (->> (element db k)
       (ancestor-keys db)
       (reduce document.h/expand-el db)))

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
                                   (:children (element db :canvas)))))

(defn selected-tags
  [db]
  (reduce #(conj %1 (:tag %2)) #{} (selected db)))

(defn select-same-tags
  [db]
  (let [selected-tags (selected-tags db)]
    (reduce (fn [db {:keys [key tag]}]
              (cond-> db
                (contains? selected-tags tag)
                (select key))) (deselect db) (vals (elements db)))))

(defn selected-sorted
  [db]
  (sort-by #(index-tree-path db %) (selected db)))

(defn selected-sorted-keys
  [db]
  (set (map :key (selected-sorted db))))

(defn select-up
  ([db multi?]
   (select-up db (last (selected-sorted db)) multi?))
  ([db el multi?]
   (let [i (index db el)]
     (select db (if (= i (dec (count (siblings db el))))
                  (:parent el)
                  (get (siblings db el) (inc i))) multi?))))

(defn select-down
  ([db multi?]
   (select-down db (first (selected-sorted db)) multi?))
  ([db el multi?]
   (let [i (index db el)]
     (select db (if (zero? i)
                  (:parent el)
                  (get (siblings db el) (dec i))) multi?))))

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
  (update-in db [:documents (:active-document db) :hovered-keys] conj k))

(defn ignore
  [db k]
  (cond-> db
    (:active-document db)
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

(defmulti intersects-with-bounds? (fn [element _] (:tag element)))

(defmethod intersects-with-bounds? :default [])

(defn lock
  ([db]
   (reduce lock db (selected-keys db)))
  ([db el-k]
   (set-prop db el-k :locked? true)))

(defn unlock
  ([db]
   (reduce unlock db (selected-keys db)))
  ([db el-k]
   (set-prop db el-k :locked? false)))

(defn bounds
  [db]
  (element/bounds (selected db)))

(defn copy
  [db]
  (assoc db
         :copied-elements (selected-sorted db)
         :copied-bounds (bounds db)))

(defn delete
  ([db]
   (reduce delete db (selected-keys db)))
  ([db k]
   (let [el (element db k)
         ;; OPTIMIZE: No need to recur to delete all children
         db (if (element/root? el) db (reduce delete db (:children el)))]
     (cond-> db
       (not (element/root? el))
       (-> (assoc-in
            (conj (path db) (:parent el) :children)
            (vec (remove #{k} (siblings db el))))
           (update-in (path db) dissoc k)
           (document.h/expand-el (:key el)))))))

(defn update-index
  [db el f & more]
  (let [siblings (siblings db el)
        index (index db el)
        new-index (apply f index more)]
    (cond-> db
      (<= 0 new-index (-> siblings count dec))
      (update-prop (:parent el) :children vec/move index new-index))))

(defn raise
  ([db]
   (reduce raise db (selected db)))
  ([db el]
   (update-index db el inc)))

(defn lower
  ([db]
   (reduce lower db (selected db)))
  ([db el]
   (update-index db el dec)))

(defn lower-to-bottom
  ([db]
   (reduce lower-to-bottom db (selected db)))
  ([db el]
   (update-index db el (fn [_] 0))))

(defn raise-to-top
  ([db]
   (reduce raise-to-top db (selected db)))
  ([db el]
   (update-index db el #(-> (siblings db el) count dec))))

(defn set-parent
  ([db k]
   (reduce #(set-parent %1 %2 k) db (selected-sorted-keys db)))
  ([db el-k k]
   (let [el (element db el-k)]
     (cond-> db
       (and el (not= el-k k) (not (:locked? el)))
       (-> (update-prop (:parent el) :children #(vec (remove #{el-k} %)))
           (update-prop k :children conj el-k)
           (set-prop el-k :parent k))))))

(defn set-parent-at-index
  [db element-key parent-key index]
  (let [siblings (:children (element db parent-key))
        last-index (count siblings)]
    (-> db
        (set-parent element-key parent-key)
        (update-prop parent-key :children vec/move last-index index))))

(defn hovered-svg
  [db]
  (let [svgs (reverse (root-svgs db))
        pointer-pos (:adjusted-pointer-pos db)]
    (or
     (some #(when (bounds/contain-point? (tools/bounds %) pointer-pos) %) svgs)
     (element db :canvas))))

(defn translate
  ([db offset]
   (reduce (fn [db el]
             (cond-> db
               :always
               (translate el offset)

               ;; TODO: Enhance auto switching parents.
               (and (= :canvas (:parent el))
                    (not (element/svg? el))
                    (single? (selected db)))
               (-> (set-parent (:key (hovered-svg db)))
                   (translate el (mat/mul (take 2 (:bounds (hovered-svg db)))
                                          -1)))))
           db
           (top-selected-ancestors db)))
  ([db el offset]
   (cond-> db
     (not (:locked? el))
     (update-el el tools/translate offset))))

(defn position
  ([db pos]
   (reduce #(position %1 %2 pos) db (top-selected-ancestors db)))
  ([db el pos]
   (cond-> db
     (not (:locked? el))
     (update-el el tools/position pos))))

(defn scale
  ([db ratio pivot-point]
   (reduce #(scale %1 %2 ratio pivot-point) db (selected db)))
  ([db el ratio pivot-point]
   (cond-> db
     (not (:locked? el))
     (update-el el tools/scale ratio (let [[x1 y1] (:bounds el)]
                                       (mat/sub pivot-point [x1 y1]))))))

(defn align
  ([db direction]
   (reduce #(align %1 %2 direction) db (selected db)))
  ([db el direction]
   (let [bounds (:bounds el)
         center (bounds/center bounds)
         parent-bounds (:bounds (parent db el))
         parent-center (bounds/center parent-bounds)
         [cx cy] (mat/sub parent-center center)
         [x1 y1 x2 y2] (mat/sub parent-bounds bounds)]
     (translate db el (case direction
                        :top [0 y1]
                        :center-vertical [0 cy]
                        :bottom [0 y2]
                        :left [x1 0]
                        :center-horizontal [cx 0]
                        :right [x2 0])))))

(defn ->path
  ([db]
   (reduce ->path db (selected db)))
  ([db el]
   (cond-> db
     (not (:locked? el)) (update-el el element/->path))))

(defn stroke->path
  ([db]
   (reduce stroke->path db (selected db)))
  ([db el]
   (cond-> db
     (not (:locked? el)) (update-el el element/stroke->path))))

(def default-props
  {:type :element
   :visible? true
   :selected? true
   :locked? false
   :children []})

(defn overlapping-svg
  [db el]
  (let [svgs (reverse (root-svgs db)) ; Reverse to select top svgs first.
        el-bounds (tools/bounds el)]
    (or
     (some #(when (bounds/contained? el-bounds (tools/bounds %)) %) svgs)
     (some #(when (bounds/intersected? el-bounds (tools/bounds %)) %) svgs)
     (element db :canvas))))

(defn create
  [db el]
  (let [key (uuid/generate)
        page (overlapping-svg db el)
        parent (or (:parent el)
                   (if (element/svg? el) :canvas (:key page)))
        new-el (map/deep-merge el default-props {:key key :parent parent})
        [x1 y1] (tools/bounds (element db parent))
        children (:content el)
        el (dissoc el :children)]
    (cond-> db
      :always
      (-> (assoc-in (conj (path db) key) new-el)
          (update-prop (:parent new-el) :children #(vec (conj % key))))

      (not (or (element/svg? new-el) (:parent el)))
      (translate [(- x1) (- y1)])

      :always
      (update-bounds key))))

(defn add
  ([db]
   (-> db
       (add (get-temp db))
       clear-temp))
  ([db element]
   (create (deselect db) element))) ; TODO: Handle children

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
  (let [selected-elements (selected-sorted db)
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
              :attrs (merge attrs {:d new-path})}))))

(defn paste-in-place
  ([db]
   (reduce paste-in-place (deselect db) (:copied-elements db)))
  ([db el]
   (create db el)))

(defn paste
  ([db]
   (reduce paste (deselect db) (:copied-elements db)))
  ([db el]
   (let [center (bounds/center (:copied-bounds db))
         el-center (bounds/center (:bounds el))
         offset (mat/sub el-center center)
         el (dissoc el :bounds)
         svg (hovered-svg db)
         [s-x1 s-y1] (tools/bounds svg)
         pointer-pos (:adjusted-pointer-pos db)
         selected (selected-keys db)]
     (reduce
      (fn [db k] (select db k))
      (cond-> db
        :always
        (-> deselect
            (create el)
            (set-parent (:key svg))
            (position (mat/add pointer-pos offset)))

        (not= :canvas (:key svg))
        (translate [(- s-x1) (- s-y1)])) selected))))

(defn duplicate-in-place
  ([db]
   (reduce duplicate-in-place (deselect db) (selected-sorted db)))
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
   (create db {:tag tag
               :attrs attrs
               :parent (:key el)})))

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
                   (update-attr el attr #(if % (-> attrs attr) disj))))
               db style-attrs)) db)))

(defn group
  [db]
  (reduce (fn [db key] (set-parent db key (first (selected-keys db))))
          (-> (deselect db)
              (create {:tag :g}))
          (selected-sorted-keys db)))

(defn inherit-attrs
  [db source-el target-el-k]
  (reduce
   (fn [db attr]
     (let [source-attr (-> source-el :attrs attr)]
       (cond-> db
         source-attr
         (update-attr (element db target-el-k)
                      attr
                      (fn [v]
                        (if (empty? (str v))
                          source-attr
                          v)))))) db spec/presentation-attrs))

(defn ungroup
  ([db]
   (reduce ungroup db (selected db)))
  ([db el]
   (cond-> db
     (and (not (:locked? el)) (= (:tag el) :g))
     (as-> db db
       (let [i (index db el)]
         (reduce
          (fn [db el-k]
            (-> db
                (set-parent-at-index el-k (:parent el) i)
                ;; Group attributes are inherited by its children, 
                ;; so we need to maintain the presentation attrs.
                (inherit-attrs el el-k)))
          db (reverse (:children el))))
       (delete db (:key el))))))

(defn manipulate-path
  ([db action]
   (reduce #(manipulate-path %1 %2 action) db (selected db)))
  ([db el action]
   (cond-> db
     (and (not (:locked? el))
          (= (:tag el) :path))
     (update-in (path db (:key el)) path/manipulate action))))

(defn ->string
  [elements]
  (reduce #(-> (tools/render-to-string %2)
               dom.server/render-to-static-markup
               (str  "\n" %)) "" elements))
