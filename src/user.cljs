(ns user
  (:require
   [ajax.core]
   [clojure.string :as str]
   [config]
   [re-frame.core :as rf]
   [re-frame.db :as db]))

(defn ^:export move
  "Moves the selected elements."
  ([offset]
   (rf/dispatch [:element/translate offset]))

  ([x y]
   (move [x y])))

(defn ^:export fill
  "Fills the selected elements."
  [color]
  (rf/dispatch [:element/fill color]))

(defn ^:export delete
  "Deletes selected elements."
  []
  (rf/dispatch [:element/delete]))

(defn ^:export copy
  "Copies the selected elements."
  []
  (rf/dispatch [:element/copy]))

(defn ^:export paste
  "Pastes the selected elements."
  []
  (rf/dispatch [:element/paste]))

(defn ^:export paste-in-place
  "Pastes the selected elements in place."
  []
  (rf/dispatch [:element/paste-in-place]))

(defn ^:export duplicate
  "Duplicates the selected elements."
  []
  (rf/dispatch [:element/duplicate-in-place]))

(defn ^:export create
  "Creates a new element."
  [el]
  (apply #(rf/dispatch [:element/create {:tag (key %)
                                         :attrs (val %)}]) el))

(defn ^:export circle
  "Creates a circle."
  ([[cx cy] r]
   (circle [cx cy] r nil))

  ([[cx cy] r attrs]
   (create {:circle (merge {:cx cx :cy cy :r r} attrs)})))

(defn ^:export rect
  "Creates a rectangle."
  ([[x y] width height]
   (rect [x y] width height nil))

  ([[x y] width height attrs]
   (create {:rect (merge {:x x :y y :width width :height height} attrs)})))

(defn ^:export line
  "Creates a line."
  ([[[x1 y1] [x2 y2]]]
   (line [x1 y1] [x2 y2] {:stroke "#000000"}))

  ([[x1 y1] [x2 y2]]
   (line [x1 y1] [x2 y2] {:stroke "#000000"}))

  ([[x1 y1] [x2 y2] attrs]
   (create {:line (merge {:x1 x1 :y1 y1 :x2 x2 :y2 y2} attrs)})))

(defn ^:export polygon
  "Creates a polygon."
  ([points]
   (polygon points {:stroke "#000000"}))

  ([points attrs]
   (create {:polygon (merge {:points (str/join " " (flatten points))} attrs)})))

(defn ^:export polyline
  "Creates a polyline."
  ([points]
   (polyline points {:stroke "#000000"}))

  ([points attrs]
   (create {:polyline (merge {:points (str/join " " (flatten points))} attrs)})))

(defn ^:export path
  "Creates a path"
  ([path-commands]
   (path path-commands {:stroke "#000000"}))

  ([path-commands attrs]
   (create {:path (merge {:d (str/join " " (flatten path-commands))} attrs)})))

(defn ^:export image
  "Creates an image"
  ([[x y] width height href]
   (image [x y] width height href nil))

  ([[x y] width height href attrs]
   (create {:image (merge {:x x :y y :width width :height height :href href} attrs)})))

(defn ^:export set-attribute
  "Sets the attribute of the selected elements."
  [k v]
  (rf/dispatch [:element/set-attribute k v]))

(defn ^:export set-fill
  "Sets the fill color of the editor."
  [color]
  (rf/dispatch [:document/set-fill color]))

(defn ^:export set-stroke
  "Sets the stroke color of the editor."
  [color]
  (rf/dispatch [:document/set-stroke color]))

(defn ^:export db
  []
  @db/app-db)

(defn ^:export document
  []
  (get-in (db) [:documents (:active-document (db))]))

(defn ^:export elements
  []
  (:elements (document)))

(defn ^:export raise
  "Raises the selected elements."
  []
  (rf/dispatch [:element/raise]))

(defn ^:export lower
  "Lowers the selected elements."
  []
  (rf/dispatch [:element/lower]))

(defn ^:export group
  "Groups the selected elements."
  []
  (rf/dispatch [:element/group]))

(defn ^:export ungroup
  "Ungroups the selected elements."
  []
  (rf/dispatch [:element/ungroup]))

(defn ^:export select-all
  "Selects all elements."
  []
  (rf/dispatch [:element/select-all]))

(defn ^:export deselect-all
  "Deselects all elements."
  []
  (rf/dispatch [:element/deselect-all]))

(defn ^:export ->path
  "Converts selected elements to paths."
  []
  (rf/dispatch [:element/->path]))

(defn ^:export stroke->path
  "Converts selected elements to paths."
  []
  (rf/dispatch [:element/stroke->path]))

(defn ^:export align
  "Aligns the selected elements to the provided direction.
   Accepted directions
   :left :right :top :bottom :center-vertical :center-horizontal"
  [direction]
  (rf/dispatch [:element/align direction]))

(defn ^:export al
  "Aligns the selected elements to the left."
  []
  (align :left))

(defn ^:export ar
  "Aligns the selected elements to the right"
  []
  (align :right))

(defn ^:export at
  "Aligns the selected elements to the top."
  [[]]
  (align :top))

(defn ^:export ab
  "Aligns the selected elements to the bottom."
  []
  (align :bottom))

(defn ^:export acv
  "Aligns the selected elements to the vertical center."
  []
  (align :center-vertical))

(defn ^:export ach
  "Aligns the selected elements to the horizontal center."
  []
  (align :center-horizontal))

(defn ^:export animate
  "Animates the selected elements."
  ([]
   (animate {}))

  ([attrs]
   (animate :animate attrs))

  ([tag attrs]
   (rf/dispatch [:element/animate tag attrs])))

(defn ^:export undo
  "Goes back in history."
  ([]
   (rf/dispatch [:history/undo]))

  ([steps]
   (rf/dispatch [:history/undo-by steps])))

(defn ^:export redo
  "Goes forward in history."
  ([]
   (rf/dispatch [:history/redo]))

  ([steps]
   (rf/dispatch [:history/redo-by steps])))

(defn ^:export unite
  "Unites the selected elements."
  []
  (rf/dispatch [:element/bool-operation :unite])
  "")

(defn ^:export ntersect
  "Intersects the selected elements."
  []
  (rf/dispatch [:element/bool-operation :intersect]))

(defn ^:export subtract
  "Subtracts the selected elements."
  []
  (rf/dispatch [:element/bool-operation :subtract]))

(defn ^:export exclude
  "Excludes the selected elements."
  []
  (rf/dispatch [:element/bool-operation :exclude]))

;; divide already refers to cljs.core/divide
(defn ^:export devide
  "Divides the selected elements."
  []
  (rf/dispatch [:element/bool-operation :divide]))

(defn ^:export exit
  "Closes the application."
  []
  (rf/dispatch [:window/close]))

(defn ^:export help
  "Lists available functions."
  []
  (doseq [x (sort-by str (vals (ns-publics 'user)))]
    (print (:name (meta x))  " - " (:doc (meta x))))
  "")

(def ^:export version config/version)

#_{:clj-kondo/ignore [:unresolved-var]}
(comment
  (dotimes [x 25]
    (circle [(+ (* x 30) 40) (+ (* (js/Math.sin x) 10) 200)]
            10
            {:fill (str "hsl(" (* x 10) " ,50% , 50%)")}))


  (ajax.core/GET
    "https://api.thecatapi.com/v1/images/search"
    {:response-format (ajax.core/json-response-format {:keywords? true})
     :handler (fn [response]
                (let [{:keys [width height url]} (first response)]
                  (image 0 0 [width height] url)))})

  (defn kitty [x y width height]
    (ajax.core/GET
      "https://api.thecatapi.com/v1/images/search"
      {:response-format (ajax.core/json-response-format {:keywords? true})
       :handler (fn [response]
                  (image [x y]
                         width
                         height
                         (:url (first response))
                         {:preserveAspectRatio "xMidYMid slice"}))}))

  (dotimes [x 8]
    (dotimes [y 6]
      (kitty (* x 100) (* y 100) 100 100))))
