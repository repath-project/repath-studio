(ns repath.studio.elements.handlers
  (:require
   [repath.studio.tools.base :as tools]
   [repath.studio.helpers :as helpers]
   [clojure.core.matrix :as matrix]
   [reagent.dom.server :refer [render-to-string]]))

(defn elements-path [db]
  [:documents (:active-document db) :elements])

(defn elements
  [db]
  (get-in db (elements-path db)))

(defn get-element
  [db key]
  (key (elements db)))

(defn page?
  [el]
  (= :page (:type el)))

(defn clear-temp
  [{active-document :active-document :as db}]
  (update-in db [:documents active-document] dissoc :temp-element))

(defn set-temp
  [{active-document :active-document :as db} element]
  (assoc-in db [:documents active-document :temp-element] element))

(defn active-page
  [{active-document :active-document :as db}]
  (get-element db (get-in db [:documents active-document :active-page])))

(defn selected-keys
  [{active-document :active-document :as db}]
  (get-in db [:documents active-document :selected-keys]))

(defn selected
  [db]
  (filter #(not (:locked? %)) (vals (select-keys (elements db) (selected-keys db)))))

(defn update-selected
  [db f]
  (assoc-in db (elements-path db) (reduce f (elements db) (selected db))))

(defn select-element
  [{active-document :active-document :as db} key]
  (cond-> db
    (page? (get-element db key)) (assoc-in [:documents active-document :active-page] key)
    :always (update-in [:documents active-document :selected-keys] conj key)))

(defn deselect
  [{active-document :active-document :as db} key]
  (update-in db [:documents active-document :selected-keys] disj key))

(defn deselect-all
  [{active-document :active-document :as db}]
  (assoc-in db [:documents active-document :selected-keys] #{}))

(defn select
  [{active-document :active-document :as db} multiselect? element]
  (if element
    (if (page? element)
      (-> db
          (assoc-in [:documents active-document :active-page] (:key element))
          (deselect-all)
          (select-element (:key element)))
      (if-not multiselect?
        (-> db
            (deselect-all)
            (select-element (:key element)))
        (if (contains? (selected-keys db) (:key element))
          (deselect db (:key element))
          (select-element db (:key element)))))
    (deselect-all db)))

(defn bounds-intersect?
  [a-bounds b-bounds]
  (if (and a-bounds b-bounds)
    (let [[a-left a-top a-right a-bottom] a-bounds
          [b-left b-top b-right b-bottom] b-bounds]
      (not (or (> b-left a-right)
               (< b-right a-left)
               (> b-top a-bottom)
               (< b-bottom a-top)))) false))

(defn bounds-contained?
  [a-bounds b-bounds]
  (if (and a-bounds b-bounds)
    (let [[a-left a-top a-right a-bottom] a-bounds
          [b-left b-top b-right b-bottom] b-bounds]
      (and (> a-left b-left)
           (> a-top b-top)
           (< a-right b-right)
           (< a-bottom b-bottom))) false))

(defn conj-by-bounds-overlap
  [db predicate path element]
  (reduce #(if (and (not (page? %2)) (predicate (tools/adjusted-bounds (elements db) %2) (tools/adjusted-bounds (elements db) element)))
             (update-in % path conj (:key %2))
             %) db (vals (elements db))))

(defmulti intersects-with-bounds? (fn [element _] (:type element)))

(defmethod intersects-with-bounds? :default [])

(defn toggle-property
  [db element-key property]
  (update-in db (conj (elements-path db) element-key property) not))

(defn set-property
  [db element-key property value]
  (assoc-in db (conj (elements-path db) element-key property) value))

(defn set-attribute
  ([db attribute value]
   (update-selected db (fn [elements element]
                         (if (attribute (tools/attributes element)) (assoc-in elements [(:key element) :attrs attribute] value) elements))))
  ([db element-key attribute value]
   (assoc-in db (conj (elements-path db) element-key :attrs attribute) value)))

(defn copy
  [db]
  (let [selected-elements (selected db)
        html (reduce #(str % (render-to-string [(:type %2) (:attrs %2)])) "" selected-elements)]
    (js/window.api.send "toMain" (clj->js {:action "writeToClipboard" :data {:html html}}))
    (assoc db :copied-elements selected-elements)))

(defn delete
  ([db key]
   (let [element (get-element db key)
         db (reduce delete db (:children element))
         parent (get-element db (:parent element))]
     (-> db
         (assoc-in (conj (elements-path db) (:parent element) :children) (vec (remove #{key} (:children parent))))
         (update-in (elements-path db) dissoc key))))
  ([db]
   (reduce delete db (selected-keys db))))

(defn children
  [elements element]
  (:children ((:parent element) elements)))

(defn update-position
  [elements element f]
  (assoc-in elements [(:parent element) :children]
            (let [children (children elements element)
                  index  (.indexOf children (:key element))] (helpers/vec-swap children index (f index)))))

(defn raise
  [elements element]
  (update-position elements element inc))

(defn lower
  [elements element]
  (update-position elements element dec))

(defn lower-to-bottom
  [elements element]
  (update-position elements element (fn [_] 0)))

(defn raise-to-top
  [elements element]
  (update-position elements element (fn [_] (dec (count (children elements element))))))

(defn move
  [db offset]
  (update-selected db (fn [elements element]
                        (assoc elements (:key element) (tools/move element offset)))))

(defn scale
  [db offset]
  (update-selected db (fn [elements element]
                        (assoc elements (:key element) (tools/scale element offset (:scale db))))))

(defn align
  [db direction]
  (update-selected db (fn [elements element]
                        (let [[x1 y1 x2 y2] (tools/bounds (elements db) element)
                              [width height] (matrix/sub [x2 y2] [x1 y1])
                              parent ((:parent element) elements)
                              [parent-x1 parent-y1 parent-x2 parent-y2] (tools/bounds (elements db) parent)
                              [parent-width parent-height] (matrix/sub [parent-x2 parent-y2] [parent-x1 parent-y1])]
                          (assoc elements (:key element) (tools/move element (case direction
                                                                               :top [0 (- y1)]
                                                                               :center-vertical [0 (- (/ parent-height 2) (+ y1 (/ height 2)))]
                                                                               :bottom [0 (- parent-height y2)]
                                                                               :left [(- x1) 0]
                                                                               :center-horizontal [(- (/ parent-width 2) (+ x1 (/ width 2))) 0]
                                                                               :right [(- parent-width x2) 0])))))))

(defn to-path
  [db]
  (reduce (fn [db element]
            (if (get-method tools/path (:type element))
              (assoc-in db (conj (elements-path db) (:key element)) (tools/to-path element))
              db)) db (selected db)))

(defn create-element
  [db parent-key element]
  (let [key (helpers/uid)
        element (helpers/deep-merge element {:key key :visible? true :parent parent-key :children []})]
    (-> db
        (assoc-in (conj (elements-path db) key) element)
        (update-in (conj (elements-path db) parent-key :children) #(vec (conj % key)))
        (select-element key))))

(defn create
  "TODO Handle child elements recursively"
  [db elements]
  (let [active-page (active-page db)
        page? (page? elements)
        parent-key (if page? :canvas (:key active-page))]
    (if elements
      (cond-> (reduce (fn [db element] (create-element db parent-key element)) (deselect-all db) (if (seq? elements) elements [elements]))
        (not page?) (move [(- (get-in active-page [:attrs :x])) (- (get-in active-page [:attrs :y]))]))
      db)))

(defn create-from-temp
  [{active-document :active-document :as db}]
  (let [temp-element (get-in db [:documents active-document :temp-element])]
    (-> db
        (create temp-element)
        (clear-temp))))

(defn paste-in-position
  [{:keys [copied-elements] :as db}]
  (reduce (fn [db element] (create-element db (if (page? (element (:parent element))) (active-page db) (:parent element))  element)) (deselect-all db) copied-elements))

(defn paste
  [db]
  (let [db (paste-in-position db)
        bounds (tools/elements-bounds (elements db) (selected db))
        [x1 y1 x2 y2] bounds
        [width height] (matrix/sub [x2 y2] [x1 y1])
        [x y] (:adjusted-mouse-pos db)]
    (move db [(- x (+ x1 (/ width 2)) ) (- y (+ y1 (/ height 2)))])))

(defn duplicate
  [db]
  (reduce (fn [db element] (create-element db (:parent element) element)) (deselect-all db) (selected db)))

(defn animate
  [db type attrs]
  (reduce (fn [db element] (create-element db (:key element) {:type type :attrs attrs})) (deselect-all db) (selected db)))

(defn paste-styles
  [{copied-elements :copied-elements :as db}]
  (if (= 1 (count copied-elements))
    ;; TODO merge attributes from multiple selected elements
    (let [attrs (:attrs (first copied-elements))]
      (update-selected db (fn [elements element]
                            (let [key (:key element)
                                  ;;  Copy all presentation attributes except transform
                                  ;;  SEE https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/Presentation
                                  style-attrs (keys (dissoc (:presentation tools/svg-attributes) :transform))]
                              (assoc-in elements [key] (assoc element :attrs (reduce (fn [updated-attrs key]
                                                                                       (if (contains? attrs key)
                                                                                         (assoc updated-attrs key (key attrs))
                                                                                         updated-attrs)) (:attrs element) style-attrs))))))) db))

(defn set-parent
  ([db element-key parent-key]
   (if (= element-key parent-key)
     db
     (-> db
         (update-in (conj (elements-path db) (:parent (get-element db element-key)) :children) #(vec (remove #{element-key} %)))
         (update-in (conj (elements-path db) parent-key :children) conj element-key)
         (assoc-in (conj (elements-path db) element-key :parent) parent-key))))
  ([db parent-key]
   (reduce (fn [db key] (set-parent db key parent-key)) db (selected-keys db))))

(defn group
  [db]
  (reduce (fn [db key]
            (set-parent db key (first (selected-keys db)))) (-> db
                                                                (deselect-all)
                                                                (create-element (:key (active-page db)) {:type :g})) (selected-keys db)))

(defn ungroup
  [db]
  (reduce (fn [db key]
            (let [element (get-element db key)]
              (if (= (:type element) :g)
                (delete (reduce (fn [db child-key] (set-parent db child-key (:parent element))) db (:children element)) key)
                db))) db (selected-keys db)))