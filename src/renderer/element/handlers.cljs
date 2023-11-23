(ns renderer.element.handlers
  (:require
   ["paper" :refer [Path]]
   [clojure.core.matrix :as mat]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.tools.base :as tools]
   [renderer.tools.path :as path]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.map :as map]
   [renderer.utils.uuid :as uuid]
   [renderer.utils.vec :as vec]))

(defn path [db]
  [:documents (:active-document db) :elements])

(defn elements
  [db]
  (get-in db (path db)))

(defn get-element
  [db k]
  (k (elements db)))

(defn active-page
  [db]
  (->> (get-in db [:documents (:active-document db) :active-page])
       (get-element db)))

(defn selected
  [db]
  (filter :selected? (vals (elements db))))

(defn selected-keys
  [db]
  (into #{} (map :key (selected db))))

(defn parent
  ([db]
   (let [selected (selected db)]
     (cond
       (empty? selected)
       (active-page db)

       (let [parents (into #{} (map :parent selected))]
         (and (first parents)
              (empty? (rest parents))
              (= (count selected)
                 (count (:children (get-element db (first parents)))))))
       (or (parent db (parent db (first selected)))
           (active-page db))

       (= (count selected) 1)
       (or (parent db (first selected))
           (active-page db))

       :else
       (active-page db))))
  ([db el]
   (when-let [parent (:parent el)]
     (get-element db parent))))

(defn pages
  [db]
  (vals (select-keys (elements db) (-> (elements db) :canvas :children))))

(defn page?
  [el]
  (= :page (:tag el)))

(defn ancestor-keys
  [db element]
  (loop [parent-element (:parent element)
         parent-keys #{}]
    (if parent-element
      (recur
       (:parent (get-element db parent-element))
       (conj parent-keys parent-element))
      parent-keys)))

(defn clear-temp
  [db]
  (update-in db [:documents (:active-document db)] dissoc :temp-element))

(defn set-temp
  [db element]
  (assoc-in db [:documents (:active-document db) :temp-element] element))

(defn get-temp
  [db]
  (get-in db [:documents (:active-document db) :temp-element]))

(defn page
  [db el]
  (if (or (not (:parent el)) (= (:tag el) :canvas))
    (active-page db)
    (if (page? el)
      el
      (recur db (parent db el)))))

(defn set-active-page
  [db key]
  (assoc-in db [:documents (:active-document db) :active-page] key))

(defn next-active-page
  [db]
  (set-active-page db (last (-> (elements db) :canvas :children))))

(defn update-selected-by
  [db f]
  (assoc-in db (path db) (reduce f (elements db) (selected db))))

(defn select-element
  [db key]
  (assoc-in db (conj (path db) key :selected?) true))

(defn deselect-element
  [db key]
  (assoc-in db (conj (path db) key :selected?) false))

(defn toggle-selected
  [db key]
  (update-in db (conj (path db) key :selected?) not))

(defn deselect-all
  [db]
  (reduce #(deselect-element %1 %2) db (keys (elements db))))

(defn select-all
  [db]
  (reduce #(select-element %1 %2) (deselect-all db) (:children (parent db))))

(defn selected-tags
  [db]
  (reduce (fn [tags element] (conj tags (:tag element))) #{} (selected db)))

(defn select-same-tags
  [db]
  (let [selected-tags (selected-tags db)]
    (reduce (fn [db element]
              (if (contains? selected-tags (:tag element))
                (select-element db (:key element))
                db)) (deselect-all db) (vals (elements db)))))

(defn invert-selection
  [db]
  (reduce (fn [db {:keys [key tag]}]
            (if (contains? #{:page :canvas} tag)
              db
              (update-in db (conj (path db) key :selected?) not)))
          db
          (vals (elements db))))

(defn select
  [db multi? el]
  (if el
    (if-not multi?
      (-> db
          deselect-all
          (select-element (:key el))
          (set-active-page (:key (page db el))))
      (toggle-selected db (:key el)))
    (deselect-all db)))

(defn hover
  [db k]
  (update-in db [:documents (:active-document db) :hovered-keys] conj k))

(defn ignore
  [db k]
  (update-in db [:documents (:active-document db) :ignored-keys] conj k))

(defn clear-hovered
  [db]
  (assoc-in db [:documents (:active-document db) :hovered-keys] #{}))

(defn clear-ignored
  [{active-document :active-document :as db}]
  (assoc-in db [:documents active-document :ignored-keys] #{}))

(defmulti intersects-with-bounds? (fn [element _] (:tag element)))

(defmethod intersects-with-bounds? :default [])

(defn toggle-property
  [db el-k k]
  (update-in db (conj (path db) el-k k) not))

(defn set-property
  ([db k v]
   (update-selected-by db #(assoc-in %1 [(:key %2) k] v)))
  ([db el k v]
   (assoc-in db (conj (path db) (:key el) k) v)))

(defn set-attribute
  ([db k v]
   (update-selected-by db (fn [elements el]
                            (if (and (not (:locked? el))
                                     (-> el tools/attributes k))
                              (assoc-in elements [(:key el) :attrs k] v)
                              elements))))
  ([db el k v]
   (let [attr-path (conj (path db) (:key el) :attrs k)]
     (cond-> db
       (get-in db attr-path) (assoc-in attr-path v)))))

(defn update-attribute
  [db k f]
  (update-selected-by db (fn [elements el]
                           (if (and (not (:locked? el))
                                    (-> el tools/attributes k))
                             (update elements
                                     (:key el)
                                     #(hierarchy/update-attr % k f))
                             elements))))

(defn lock
  [db]
  (update-selected-by db #(assoc-in %1 [(:key %2) :locked?] true)))

(defn unlock
  [db]
  (update-selected-by db #(assoc-in %1 [(:key %2) :locked?] false)))

(defn copy
  [db]
  (assoc db :copied-elements (selected db)))

(defn delete
  ([db]
   (reduce delete db (selected-keys db)))
  ([db k]
   (let [element (get-element db k)
         db (reduce delete db (:children element))]
     (cond-> db
       :always
       (assoc-in
        (conj (path db) (:parent element) :children)
        (vec (remove #{k} (:children (parent db element)))))

       (page? element)
       (next-active-page)

       :always
       (update-in (path db) dissoc k)))))

(defn siblings-keys
  [elements el]
  (:children ((:parent el) elements)))

(defn update-index
  [elements el f]
  (let [children (siblings-keys elements el)
        index (.indexOf children (:key el))]
    (assoc-in elements
              [(:parent el) :children]
              (vec/swap children index (f index)))))

(defn raise
  [elements el]
  (update-index elements el inc))

(defn lower
  [elements el]
  (update-index elements el dec))

(defn lower-to-bottom
  [elements el]
  (update-index elements el (fn [_] 0)))

(defn raise-to-top
  [elements el]
  (update-index elements el #(-> (siblings-keys elements el)
                                 count
                                 dec)))

(defn translate
  [db offset]
  (update-selected-by db (fn [elements el]
                           (assoc elements
                                  (:key el)
                                  (tools/translate el offset)))))

(defn scale
  [db offset _lock-ratio? _in-place?]
  (let [[x1 y1 x2 y2] (tools/elements-bounds (elements db) (selected db))
        outer-dimensions (bounds/->dimensions [x1 y1 x2 y2])
        handler (-> db :clicked-element :key)]
    (update-selected-by
     db
     (fn [elements element]
       (let [[inner-x1 inner-y1 inner-x2 inner-y2] (tools/bounds element (elements db))
             inner-dimensions (bounds/->dimensions [inner-x1 inner-y1 inner-x2 inner-y2])
             scale-multiplier (mat/div inner-dimensions outer-dimensions)
             translate-multiplier (mat/div (case handler
                                             :middle-right [(- inner-x1 x1) 0]
                                             :middle-left [(- x2 inner-x2) 0]
                                             :top-middle [0 (- y2 inner-y2)]
                                             :bottom-middle [0 (- inner-y1 y1)]
                                             :top-right [(- inner-x1 x1) (- y2 inner-y2)]
                                             :top-left [(- x2 inner-x2) (- y2 inner-y2)]
                                             :bottom-right [(- inner-x1 x1) (- inner-y1 y1)]
                                             :bottom-left [(- x2 inner-x2) (- inner-y1 y1)]) outer-dimensions)]
         (assoc elements (:key element) (-> element
                                            (tools/scale (mat/mul offset scale-multiplier) handler)
                                            (tools/translate (mat/mul offset translate-multiplier)))))))))

(defn align
  ([db direction]
   (update-selected-by
    db
    (fn [elements el]
      (let [[x1 y1 x2 y2] (tools/bounds el (elements db))
            [width height] (bounds/->dimensions [x1 y1 x2 y2])
            parent ((:parent el) elements)
            [parent-x1 parent-y1 parent-x2 parent-y2] (tools/bounds parent (elements db))
            [parent-width parent-height] (mat/sub [parent-x2 parent-y2] [parent-x1 parent-y1])]
        (assoc elements
               (:key el)
               (tools/translate el
                                (case direction
                                  :top [0 (- y1)]
                                  :center-vertical [0 (- (/ parent-height 2)
                                                         (+ y1 (/ height 2)))]
                                  :bottom [0 (- parent-height y2)]
                                  :left [(- x1) 0]
                                  :center-horizontal [(- (/ parent-width 2)
                                                         (+ x1 (/ width 2))) 0]
                                  :right [(- parent-width x2) 0]))))))))

(defn ->path
  ([db]
   (reduce #(->path %1 %2) db (selected db)))
  ([db el]
   (if (get-method tools/path (:tag el))
     (assoc-in db (conj (path db) (:key el)) (tools/->path el))
     db)))

(defn stroke->path
  ([db]
   (reduce #(stroke->path %1 %2) db (selected db)))
  ([db el]
   (if (get-method tools/stroke->path (:tag el))
     (assoc-in db (conj (path db) (:key el)) (tools/stroke->path el))
     db)))

(defn create-element
  [db parent-key el]
  (let [key (uuid/generate)
        el (map/deep-merge el {:key key
                               :type :element
                               :visible? true
                               :selected? true
                               :parent parent-key
                               :children []})]
    (cond-> db
      :always
      (-> (assoc-in (conj (path db) key) el)
          (update-in (conj (path db) parent-key :children)
                     #(vec (conj % key))))

      (not= (:tool db) :select)
      (tools/set-tool :select)

      (page? el)
      (set-active-page key))))

;; TODO: Handle child elements
(defn create
  ([db]
   (-> db
       (create (get-temp db))
       clear-temp))
  ([db elements]
   (let [active-page (active-page db)
         page? (page? elements)
         parent-key (if page? :canvas (:key active-page))]
     (if elements
       (cond-> (reduce (fn [db el] (create-element db parent-key el))
                       (deselect-all db)
                       (if (seq? elements) elements [elements]))
         (not page?)
         (translate [(- (get-in active-page [:attrs :x])) (- (get-in active-page [:attrs :y]))]))
       db))))

(defn bool-operation
  [db operation]
  (let [selected-elements (selected db) ; TODO: sort elements by visibily index
        attrs (-> selected-elements first tools/->path :attrs)
        new-path (reduce (fn [path element]
                           (let [path-a (Path. path)
                                 path-b (-> element tools/->path :attrs :d Path.)
                                 result-path (case operation
                                               :unite (.unite path-a path-b)
                                               :intersect (.intersect path-a path-b)
                                               :subtract (.subtract path-a path-b)
                                               :exclude (.exclude path-a path-b)
                                               :divide (.divide path-a path-b))]
                             (.getAttribute (.exportSVG result-path) "d")))
                         (:d attrs)
                         (rest selected-elements))]
    (-> db
        delete
        (create {:type :element
                 :tag :path
                 :attrs (merge attrs {:d new-path})}))))

(defn paste-in-place
  ([db]
   (reduce paste-in-place (deselect-all db) (:copied-elements db)))
  ([db el]
   (create-element db (if (page? (get-element db (:parent el)))
                        (-> db active-page :key)
                        (:parent el)) el)))

(defn paste
  [db]
  (let [db (paste-in-place db)
        bounds (tools/elements-bounds (elements db) (selected db))
        [x1 y1] bounds
        [width height] (bounds/->dimensions bounds)
        [x y] (:adjusted-mouse-pos db)]
    (translate db [(- x (+ x1 (/ width 2)))
                   (- y (+ y1 (/ height 2)))])))

(defn duplicate-in-place
  ([db]
   (reduce duplicate-in-place (deselect-all db) (selected db)))
  ([db el]
   (create-element db (:parent el) el)))

(defn duplicate
  [db offset]
  (-> db
      duplicate-in-place
      (translate offset)))

(defn animate
  ([db tag attrs]
   (reduce #(animate %1 %2 tag attrs) (deselect-all db) (selected db)))
  ([db el tag attrs]
   (create-element db (:key el) {:tag tag :attrs attrs})))

(defn paste-styles
  [{copied-elements :copied-elements :as db}]
  (if (= 1 (count copied-elements))
    ;; TODO: Merge attributes from multiple selected elements.
    (let [attrs (:attrs (first copied-elements))]
      (update-selected-by
       db
       (fn [elements element]
         (let [key (:key element)
               ;; Copy all presentation attributes except transform.
               ;; https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/Presentation
               style-attrs (-> tools/svg-spec
                               :attributes
                               :presentation
                               (dissoc :transform)
                               keys)]
           (assoc-in
            elements
            [key]
            (assoc element
                   :attrs
                   (reduce (fn [updated-attrs key]
                             (if (contains? attrs key)
                               (assoc updated-attrs key (key attrs))
                               updated-attrs))
                           (:attrs element)
                           style-attrs))))))) db))

(defn set-parent
  ([db element-key parent-key]
   (if (= element-key parent-key)
     db
     (-> db
         (update-in (conj (path db)
                          (:parent (get-element db element-key)) :children)
                    #(vec (remove #{element-key} %)))
         (update-in (conj (path db)
                          parent-key :children) conj element-key)
         (assoc-in (conj
                    (path db) element-key :parent)
                   parent-key))))
  ([db parent-key]
   (reduce (fn [db key] (set-parent db key parent-key)) db (selected-keys db))))

(defn group
  [db]
  (reduce (fn [db key]
            (set-parent db key (first (selected-keys db))))
          (-> db
              deselect-all
              (create-element (:key (active-page db)) {:tag :g}))
          (selected-keys db)))

(defn ungroup
  [db]
  (reduce (fn [db key]
            (let [element (get-element db key)]
              (if (= (:tag element) :g)
                (delete (reduce (fn [db child-key]
                                  (set-parent db child-key (:parent element)))
                                db
                                (:children element))
                        key)
                db)))
          db
          (selected-keys db)))

(defn manipulate-path
  [db action]
  (update-selected-by db (fn [elements element]
                           (if (= (:tag element) :path)
                             (assoc elements
                                    (:key element)
                                    (path/manipulate element action))
                             elements))))
