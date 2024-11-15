(ns renderer.element.handlers
  (:require
   [clojure.core.matrix :as mat]
   [clojure.set :as set]
   [clojure.string :as str]
   [hickory.core :as hickory]
   [hickory.zip]
   [malli.core :as m]
   [malli.error :as me]
   [renderer.app.db :refer [App]]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.db :as db :refer [Element Tag AnimationTag Direction]]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.utils.attribute :as attr]
   [renderer.utils.bounds :as bounds :refer [Bounds]]
   [renderer.utils.element :as element]
   [renderer.utils.extra :refer [partial-right]]
   [renderer.utils.hiccup :as hiccup]
   [renderer.utils.map :as map]
   [renderer.utils.math :as math :refer [Vec2D]]
   [renderer.utils.path :as path :refer [PathManipulation PathBooleanOperation]]
   [renderer.utils.vec :as vec]))

(m/=> path [:-> App [:* any?] vector?])
(defn path
  [db & more]
  (apply conj [:documents (:active-document db) :elements] more))

(m/=> entities [:function
                [:-> App [:maybe [:map-of uuid? Element]]]
                [:-> App [:maybe [:set uuid?]] [:maybe [:map-of uuid? Element]]]])
(defn entities
  ([db]
   (get-in db (path db)))
  ([db ids]
   (select-keys (entities db) (vec ids))))

(m/=> entity [:-> App [:maybe uuid?] [:maybe Element]])
(defn entity
  [db id]
  (get (entities db) id))

(m/=> root [:-> App Element])
(defn root
  [db]
  (some #(when (element/root? %) %) (vals (entities db))))

(m/=> locked? [:-> App uuid? boolean?])
(defn locked?
  [db id]
  (-> db (entity id) :locked boolean))

(m/=> selected [:-> App [:sequential Element]])
(defn selected
  [db]
  (->> db entities vals (filter :selected)))

(m/=> ratio-locked? [:-> App boolean?])
(defn ratio-locked?
  [db]
  (every? element/ratio-locked? (selected db)))

(m/=> selected-ids [:-> App [:set uuid?]])
(defn selected-ids
  [db]
  (->> db selected (map :id) set))

(m/=> children-ids [:-> App uuid? [:vector uuid?]])
(defn children-ids
  [db id]
  (:children (entity db id)))

(m/=> parent-ids [:-> App [:set uuid?]])
(defn parent-ids
  [db]
  (->> (selected db)
       (map :parent)
       (remove nil?)
       (set)))

(m/=> parent [:function
              [:-> App [:maybe Element]]
              [:-> App [:maybe uuid?] [:maybe Element]]])
(defn parent
  ([db]
   (let [ids (parent-ids db)]
     (if (= (count ids) 1)
       (entity db (first ids))
       (root db))))
  ([db id]
   (when-let [parent-id (:parent (entity db id))]
     (entity db parent-id))))

(m/=> parent-container [:-> App uuid? Element])
(defn parent-container
  [db id]
  (loop [parent-el (parent db id)]
    (when parent-el
      (if (element/container? parent-el)
        parent-el
        (recur (parent db (:id parent-el)))))))

(m/=> adjusted-bounds [:-> App uuid? [:maybe Bounds]])
(defn adjusted-bounds
  [db id]
  (when-let [bounds (hierarchy/bounds (entity db id))]
    (if-let [container (parent-container db id)]
      (let [[offset-x offset-y _ _] (hierarchy/bounds container)
            [x1 y1 x2 y2] bounds]
        [(+ x1 offset-x) (+ y1 offset-y) (+ x2 offset-x) (+ y2 offset-y)])
      bounds)))

(m/=> refresh-bounds [:-> App uuid? App])
(defn refresh-bounds
  [db id]
  (let [el (entity db id)
        bounds (if (= (:tag el) :g)
                 (let [b (map #(adjusted-bounds db %) (children-ids db id))]
                   (when (seq b) (apply bounds/union b)))
                 (adjusted-bounds db id))]
    (if (or (not bounds) (element/root? el))
      db
      (-> (reduce refresh-bounds db (children-ids db id))
          (update-in (path db id) assoc :bounds bounds)))))

(m/=> update-el [:-> App uuid? ifn? [:* any?] App])
(defn update-el
  [db id f & more]
  (if (locked? db id)
    db
    (-> (apply update-in db (path db id) f more)
        (refresh-bounds id))))

(m/=> siblings-selected? [:-> App [:maybe boolean?]])
(defn siblings-selected?
  [db]
  (let [selected-els (selected db)
        parent-els (set (map :parent selected-els))]
    (and (seq parent-els)
         (empty? (rest parent-els))
         (= (count selected-els)
            (count (children-ids db (first parent-els)))))))

(m/=> siblings [:function
                [:-> App [:maybe [:vector uuid?]]]
                [:-> App uuid? [:maybe [:vector uuid?]]]])
(defn siblings
  ([db]
   (:children (parent db)))
  ([db id]
   (:children (parent db id))))

(m/=> root-children [:-> App [:sequential Element]])
(defn root-children
  [db]
  (->> (children-ids db (:id (root db)))
       (mapv (entities db))))

(m/=> root-svgs [:-> App [:sequential Element]])
(defn root-svgs
  [db]
  (->> db root-children (filter element/svg?)))

(m/=> ancestor-ids [:function
                    [:-> App [:sequential uuid?]]
                    [:-> App uuid? [:sequential uuid?]]])
(defn ancestor-ids
  ([db]
   (reduce #(concat %1 (ancestor-ids db %2)) [] (selected-ids db)))
  ([db id]
   (loop [parent-id (:parent (entity db id))
          ids []]
     (if parent-id
       (recur
        (:parent (entity db parent-id))
        (conj ids parent-id))
       ids))))

(m/=> index [:-> App uuid? [:maybe int?]])
(defn index
  "Returns the index of an element on its parent children vector."
  [db id]
  (when-let [sibling-els (siblings db id)]
    (.indexOf sibling-els id)))

(m/=> index-path [:-> App uuid? [:sequential int?]])
(defn index-path
  "Returns a sequence that represents the index path of an element.
   For example, the seventh element of the second svg on the canvas will
   return [2 7]. This is useful when we need to figure out the global index
   of nested elements."
  [db id]
  (let [ancestor-els (reverse (ancestor-ids db id))]
    (->> (index db id)
         (conj (map #(index db %) ancestor-els))
         (remove nil?)
         (vec))))

(m/=> descendant-ids [:function
                      [:-> App [:set uuid?]]
                      [:-> App uuid? [:set uuid?]]])
(defn descendant-ids
  ([db]
   (reduce #(set/union %1 (descendant-ids db %2)) #{} (selected-ids db)))
  ([db id]
   (loop [children-set (set (children-ids db id))
          child-keys #{}]
     (if (seq children-set)
       (recur
        (reduce #(set/union %1 (set (children-ids db %2))) #{} children-set)
        (set/union child-keys children-set))
       child-keys))))

(m/=> top-ancestor-ids [:-> App [:set uuid?]])
(defn top-ancestor-ids
  [db]
  (set/difference (selected-ids db) (descendant-ids db)))

(m/=> selected-with-descendant-ids [:-> App [:set uuid?]])
(defn selected-with-descendant-ids
  [db]
  (set/union (selected-ids db) (descendant-ids db)))

(m/=> non-selected-ids [:-> App [:maybe [:set uuid?]]])
(defn non-selected-ids
  [db]
  (set/difference (-> db entities keys set) (selected-with-descendant-ids db)))

(m/=> top-selected-ancestors [:-> App [:sequential Element]])
(defn top-selected-ancestors
  [db]
  (->> (top-ancestor-ids db)
       (entities db)
       (vals)))

(defn update-prop
  [db id k & more]
  (-> (apply update-in db (path db id k) more)
      (refresh-bounds id)))

(m/=> assoc-prop [:function
                  [:-> App keyword? any? App]
                  [:-> App uuid? keyword? any? App]])
(defn assoc-prop
  ([db k v]
   (reduce (partial-right assoc-prop k v) db (selected-ids db)))
  ([db id k v]
   (-> (if (str/blank? v)
         (update-in db (path db id) dissoc k)
         (assoc-in db (path db id k) v))
       (refresh-bounds id))))

(m/=> dissoc-attr [:function
                   [:-> App keyword? App]
                   [:-> App uuid? keyword? App]])
(defn dissoc-attr
  ([db k]
   (reduce (partial-right dissoc-attr k) db (selected-ids db)))
  ([db id k]
   (cond-> db
     (not (locked? db id))
     (update-prop id :attrs dissoc k))))

(m/=> assoc-attr [:function
                  [:-> App keyword? string? App]
                  [:-> App uuid? keyword? string? App]])
(defn assoc-attr
  ([db k v]
   (reduce (partial-right assoc-attr k v) db (selected-ids db)))
  ([db id k v]
   (cond-> db
     (not (locked? db id))
     (-> (assoc-in (path db id :attrs k) v)
         (refresh-bounds id)))))

(m/=> set-attr [:function
                [:-> App keyword? any? App]
                [:-> App uuid? keyword? any? App]])
(defn set-attr
  ([db k v]
   (reduce (partial-right set-attr k v) db (selected-ids db)))
  ([db id k v]
   (if (and (not (locked? db id))
            (element/supported-attr? (entity db id) k))
     (if (str/blank? v)
       (dissoc-attr db id k)
       (assoc-attr db id k (str/trim (str v))))
     db)))

(defn update-attr
  [db id k f & more]
  (if (element/supported-attr? (entity db id) k)
    (apply update-el db id attr.hierarchy/update-attr k f more)
    db))

(m/=> deselect [:-> App uuid? App])
(defn deselect
  [db id]
  (assoc-prop db id :selected false))

(m/=> deselect-all [:-> App App])
(defn deselect-all
  [db]
  (reduce deselect db (selected-ids db)))

(m/=> collapse [:-> App uuid? App])
(defn collapse
  [db id]
  (update-in db [:documents (:active-document db) :collapsed-ids] conj id))

(m/=> collapse-all [:-> App App])
(defn collapse-all
  [db]
  (reduce collapse db (keys (entities db))))

(m/=> expand [:-> App uuid? App])
(defn expand
  [db id]
  (update-in db [:documents (:active-document db) :collapsed-ids] disj id))

(m/=> expand-ancestors [:-> App uuid? App])
(defn expand-ancestors
  [db id]
  (->> (ancestor-ids db id)
       (reduce expand db)))

(m/=> select [:-> App uuid? App])
(defn select
  [db id]
  (-> db
      (expand-ancestors id)
      (assoc-prop id :selected true)))

(m/=> toggle-selection [:-> App uuid? boolean? App])
(defn toggle-selection
  [db id multiple]
  (if (entity db id)
    (if multiple
      (update-prop db id :selected not)
      (-> db deselect-all (select id)))
    (deselect-all db)))

(m/=> select-all [:-> App App])
(defn select-all
  [db]
  (reduce select db (if (siblings-selected? db)
                      (children-ids db (:id (parent db (:id (parent db)))))
                      (siblings db))))

(m/=> selected-tags [:-> App [:set Tag]])
(defn selected-tags
  [db]
  (->> (selected db)
       (map :tag)
       (set)))

(m/=> filter-by-tag [:-> App Tag [:sequential Element]])
(defn filter-by-tag
  [db tag]
  (filter #(= tag (:tag %)) (selected db)))

(m/=> select-same-tags [:-> App App])
(defn select-same-tags
  [db]
  (let [tags (selected-tags db)]
    (->> (entities db)
         (vals)
         (reduce (fn [db el] (cond-> db
                               (contains? tags (:tag el))
                               (select (:id el)))) db))))

(m/=> selected-sorted [:-> App [:sequential Element]])
(defn selected-sorted
  [db]
  (sort-by #(index-path db (:id %)) (selected db)))

(m/=> top-selected-sorted [:-> App [:sequential Element]])
(defn top-selected-sorted
  [db]
  (sort-by #(index-path db (:id %)) (top-selected-ancestors db)))

(m/=> selected-sorted-ids [:-> App [:vector uuid?]])
(defn selected-sorted-ids
  [db]
  (mapv :id (selected-sorted db)))

(m/=> top-selected-sorted-ids [:-> App [:vector uuid?]])
(defn top-selected-sorted-ids
  [db]
  (mapv :id (top-selected-sorted db)))

(m/=> invert-selection [:-> App App])
(defn invert-selection
  [db]
  (reduce (fn [db {:keys [id tag]}]
            (cond-> db
              (not (contains? #{:svg :canvas} tag))
              (update-prop id :selected not)))
          db
          (vals (entities db))))

(m/=> hover [:-> App [:or uuid? keyword?] App])
(defn hover
  [db id]
  (update-in db [:documents (:active-document db) :hovered-ids] conj id))

(m/=> ignore [:-> App [:or uuid? keyword?] App])
(defn ignore
  [db id]
  (cond-> db
    (and (:active-document db) id)
    (update-in [:documents (:active-document db) :ignored-ids] conj id)))

(m/=> clear-hovered [:-> App App])
(defn clear-hovered
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in [:documents (:active-document db) :hovered-ids] #{})))

(m/=> unignore [:-> App [:or uuid? keyword?] App])
(defn unignore
  [db id]
  (cond-> db
    (:active-document db)
    (update-in [:documents (:active-document db) :ignored-ids] disj id)))

(m/=> clear-ignored [:-> App App])
(defn clear-ignored
  [db]
  (cond-> db
    (:active-document db)
    (assoc-in [:documents (:active-document db) :ignored-ids] #{})))

(m/=> bounds [:-> App [:maybe Bounds]])
(defn bounds
  [db]
  (element/united-bounds (selected db)))

(m/=> copy [:-> App App])
(defn copy
  [db]
  (let [els (top-selected-sorted db)]
    (cond-> db
      (seq els)
      (assoc :copied-elements els
             :copied-bounds (bounds db)))))

(m/=> delete [:function
              [:-> App App]
              [:-> App uuid? App]])
(defn delete
  ([db]
   (reduce delete db (reverse (selected-sorted-ids db))))
  ([db id]
   (let [el (entity db id)
         db (if (element/root? el) db (reduce delete db (:children el)))]
     (cond-> db
       (not (element/root? el))
       (-> (update-prop (:parent el) :children vec/remove-nth (index db id))
           (update-in (path db) dissoc id)
           (expand id))))))

(m/=> update-index [:function
                    [:-> App ifn? App]
                    [:-> App uuid? ifn? App]])
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

(m/=> set-parent [:function
                  [:-> App uuid? App]
                  [:-> App uuid? uuid? App]])
(defn set-parent
  ([db parent-id]
   (reduce (partial-right set-parent parent-id) db (selected-sorted-ids db)))
  ([db id parent-id]
   (let [el (entity db id)]
     (cond-> db
       (and el (not= id parent-id) (not (locked? db id)))
       (-> (update-prop (:parent el) :children #(vec (remove #{id} %)))
           (update-prop parent-id :children conj id)
           (assoc-prop id :parent parent-id))))))

(m/=> set-parent-at-index [:-> App uuid? uuid? int? App])
(defn set-parent-at-index
  [db id parent-id i]
  (let [sibling-els (:children (entity db parent-id))
        last-index (count sibling-els)]
    (-> db
        (set-parent id parent-id)
        (update-prop parent-id :children vec/move last-index i))))

(m/=> hovered-svg [:-> App Element])
(defn hovered-svg
  [db]
  (let [svgs (reverse (root-svgs db))
        pointer-pos (:adjusted-pointer-pos db)]
    (or
     (some #(when (bounds/contained-point? (:bounds %) pointer-pos) %) svgs)
     (root db))))

(m/=> translate [:function
                 [:-> App Vec2D App]
                 [:-> App uuid? Vec2D App]])
(defn translate
  "Moves elements by a given offset."
  ([db offset]
   (reduce (partial-right translate offset) db (top-ancestor-ids db)))
  ([db id offset]
   (update-el db id hierarchy/translate offset)))

(m/=> place [:function
             [:-> App Vec2D App]
             [:-> App uuid? Vec2D App]])
(defn place
  "Positions elements to a given global position."
  ([db position]
   (reduce (partial-right place position) db (top-ancestor-ids db)))
  ([db id position]
   (let [el (entity db id)
         center (bounds/center (hierarchy/bounds el))
         offset (mat/sub position center)]
     (update-el db id hierarchy/translate offset))))

(m/=> scale [:-> App Vec2D Vec2D boolean? App])
(defn scale
  [db ratio pivot-point recursive]
  (let [ids-to-scale (cond-> (selected-ids db) recursive (set/union (descendant-ids db)))]
    (reduce
     (fn [db id]
       (let [pivot-point (->> (entity db id) :bounds (take 2) (mat/sub pivot-point))]
         (update-el db id hierarchy/scale ratio pivot-point)))
     db
     ids-to-scale)))

(m/=> align [:function
             [:-> App Direction App]
             [:-> App uuid? Direction App]])
(defn align
  ([db direction]
   (reduce (partial-right align direction) db (selected-ids db)))
  ([db id direction]
   (let [el-bounds (:bounds (entity db id))
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

(m/=> ->path [:function
              [:-> App App]
              [:-> App uuid? App]])
(defn ->path
  "Converts elements to paths."
  ([db]
   (reduce ->path db (selected-ids db)))
  ([db id]
   (update-el db id element/->path)))

(m/=> stroke->path [:function
                    [:-> App App]
                    [:-> App uuid? App]])
(defn stroke->path
  "Converts the stroke of elements to paths."
  ([db]
   (reduce stroke->path db (selected-ids db)))
  ([db id]
   (update-el db id element/stroke->path)))

(m/=> overlapping-svg [:-> App Bounds Element])
(defn overlapping-svg
  [db el-bounds]
  (let [svgs (reverse (root-svgs db))] ; Reverse to select top svgs first.
    (or
     (some #(when (bounds/contained? el-bounds (:bounds %)) %) svgs)
     (some #(when (bounds/intersect? el-bounds (:bounds %)) %) svgs)
     (root db))))

(m/=> assoc-parent-id [:-> App Element Element])
(defn assoc-parent-id
  [db el]
  (cond-> el
    (not (or (element/root? el) (:parent el)))
    (assoc :parent (:id (if (element/svg? el)
                          (root db)
                          (overlapping-svg db (hierarchy/bounds el)))))))

(m/=> create [:-> App map? App])
(defn create
  [db el]
  (let [id (random-uuid) ; REVIEW: Hard to use a coeffect because of recursion.
        new-el (->> (cond-> el (not (string? (:content el))) (dissoc :content))
                    (map/remove-nils)
                    (element/normalize-attrs)
                    (assoc-parent-id db))
        new-el (merge new-el db/default {:id id})
        child-els (-> (entities db (set (:children el))) vals (concat (:content el)))
        [x1 y1] (hierarchy/bounds (entity db (:parent new-el)))
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
        (assoc-in (path db id) new-el)

        (:parent new-el)
        (update-prop (:parent new-el) :children #(vec (conj % id)))

        (not (or (element/svg? new-el) (element/root? new-el) (:parent el)))
        (translate [(- x1) (- y1)])

        :always
        (refresh-bounds id)

        child-els
        (add-children child-els)))))

(m/=> create-default-canvas [:-> App [:maybe Vec2D] App])
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

(m/=> add [:-> App map? App])
(defn add
  [db el]
  (-> (deselect-all db)
      (create (assoc el :selected true))))

(m/=> boolean-operation [:-> App PathBooleanOperation App])
(defn boolean-operation
  [db operation]
  (let [selected-elements (top-selected-sorted db)
        attrs (-> selected-elements first element/->path :attrs)
        new-path (reduce (fn [path-a el]
                           (let [path-b (-> el element/->path :attrs :d)]
                             (path/boolean-operation path-a path-b operation)))
                         (:d attrs)
                         (rest selected-elements))]
    (cond-> db
      (seq new-path)
      (-> (delete)
          (add {:type :element
                :tag :path
                :parent (-> selected-elements first :parent)
                :attrs (merge attrs {:d new-path})})))))

(m/=> paste-in-place [:function
                      [:-> App App]
                      [:-> App Element App]])
(defn paste-in-place
  ([db]
   (reduce paste-in-place (deselect-all db) (:copied-elements db)))
  ([db el]
   (reduce select (add db el) (selected-ids db))))

(m/=> paste [:function
             [:-> App App]
             [:-> App Element Element App]])
(defn paste
  ([db]
   (let [parent-el (hovered-svg db)]
     (reduce (partial-right paste parent-el) (deselect-all db) (:copied-elements db))))
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
        (-> (deselect-all)
            (add (assoc el :parent (:id parent-el)))
            (place (mat/add pointer-pos offset)))

        (not= (:id (root db)) (:id parent-el))
        (translate [(- s-x1) (- s-y1)])) (selected-ids db)))))

(m/=> duplicate [:-> App App])
(defn duplicate
  [db]
  (reduce create (deselect-all db) (top-selected-sorted db)))

(m/=> animate [:function
               [:-> App AnimationTag App]
               [:-> App AnimationTag map? App]
               [:-> App uuid? AnimationTag map? App]])
(defn animate
  ([db tag]
   (animate db tag {}))
  ([db tag attrs]
   (reduce (partial-right animate tag attrs) (deselect-all db) (selected-ids db)))
  ([db id tag attrs]
   (reduce select (add db {:tag tag
                           :attrs attrs
                           :parent id}) (selected-ids db))))

(m/=> paste-styles [:function
                    [:-> App App]
                    [:-> App uuid? App]])
(defn paste-styles
  ([db]
   (reduce paste-styles db (selected-ids db)))
  ([db id]
   ;; TODO: Merge attributes from multiple selected elements.
   (if (= 1 (count (:copied-elements db)))
     (let [attrs (-> db :copied-elements first :attrs)
           style-attrs (disj attr/presentation :transform)]
       (reduce (fn [db attr]
                 (cond-> db
                   (attr attrs)
                   (update-attr id attr #(if % (-> attrs attr) disj))))
               db style-attrs)) db)))

(m/=> inherit-attrs [:-> App Element uuid? App])
(defn inherit-attrs
  [db source-el id]
  (reduce
   (fn [db attr]
     (let [source-attr (-> source-el :attrs attr)
           get-value (fn [v] (if (empty? (str v)) source-attr v))]
       (cond-> db
         source-attr
         (update-attr id attr get-value)))) db attr/presentation))

(m/=> group [:function
             [:-> App App]
             [:-> App [:sequential uuid?] App]])
(defn group
  ([db]
   (group db (top-selected-sorted-ids db)))
  ([db ids]
   (reduce (fn [db id] (set-parent db id (-> db selected-ids first)))
           (add db {:tag :g
                    :parent (:id (parent db))}) ids)))

(m/=> ungroup [:function
               [:-> App App]
               [:-> App uuid? App]])
(defn ungroup
  ([db]
   (reduce ungroup db (selected-ids db)))
  ([db id]
   (cond-> db
     (and (not (locked? db id)) (= (:tag (entity db id)) :g))
     (as-> db db
       (let [i (index db id)]
         (reduce
          (fn [db child-id]
            (-> db
                (set-parent-at-index child-id (:parent (entity db id)) i)
                ;; Group attributes are inherited by its children,
                ;; so we need to maintain the presentation attrs.
                (inherit-attrs (entity db id) child-id)
                (select child-id)))
          db (reverse (children-ids db id))))
       (delete db id)))))

(m/=> manipulate-path [:function
                       [:-> App PathManipulation App]
                       [:-> App uuid? PathManipulation App]])
(defn manipulate-path
  ([db action]
   (reduce (partial-right manipulate-path action) db (selected-ids db)))
  ([db id action]
   (cond-> db
     (= (:tag (entity db id)) :path)
     (update-attr id :d path/manipulate action))))

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
        (collapse-all))))

(m/=> snapping-points [:-> App [:maybe [:sequential Element]] [:vector Vec2D]])
(defn snapping-points
  [db els]
  (let [options (-> db :snap :options)]
    (reduce (fn [points el]
              (into points (element/snapping-points el options))) [] els)))
