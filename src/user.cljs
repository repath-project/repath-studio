(ns user
  {:clj-kondo/config {:linters {:unused-public-var {:level :off}}}}
  (:require
   [re-frame.core :as rf]
   [re-frame.db :as db]
   [config]
   [clojure.string :as str]
   [ajax.core]))

(defn move
  "Moves the selected elements."
  ([offset]
   (rf/dispatch [:elements/translate offset]))

  ([x y]
   (move [x y])))

(defn fill
  "Fills the selected elements."
  [color]
  (rf/dispatch [:elements/fill color]))

(defn delete
  "Deletes selected elements."
  []
  (rf/dispatch [:elements/delete]))

(defn copy
  "Copies the selected elements."
  []
  (rf/dispatch [:elements/copy]))

(defn paste
  "Pastes the selected elements."
  []
  (rf/dispatch [:elements/paste]))

(defn paste-in-place
  "Pastes the selected elements in place."
  []
  (rf/dispatch [:elements/paste-in-place]))

(defn duplicate
  "Duplicates the selected elements."
  []
  (rf/dispatch [:elements/duplicate-in-place]))

(defn create
  "Creates a new element."
  [element]
  (apply #(rf/dispatch [:elements/create {:tag (key %)
                                          :attrs (val %)}]) element))

(defn circle
  "Creates a circle."
  ([[cx cy] r]
   (circle [cx cy] r nil))

  ([[cx cy] r attrs]
   (create {:circle (merge {:cx cx :cy cy :r r} attrs)})))

(defn rect
  "Creates a rectangle."
  ([[x y] width height]
   (rect [x y] width height nil))

  ([[x y] width height attrs]
   (create {:rect (merge {:x x :y y :width width :height height} attrs)})))

(defn line
  "Creates a line."
  ([[[x1 y1] [x2 y2]]]
   (line [x1 y1] [x2 y2] {:stroke "#000000"}))

  ([[x1 y1] [x2 y2]]
   (line [x1 y1] [x2 y2] {:stroke "#000000"}))

  ([[x1 y1] [x2 y2] attrs]
   (create {:line (merge {:x1 x1 :y1 y1 :x2 x2 :y2 y2} attrs)})))

(defn polygon
  "Creates a polygon."
  ([points]
   (polygon points {:stroke "#000000"}))

  ([points attrs]
   (create {:polygon (merge {:points (str/join " " (flatten points))} attrs)})))

(defn polyline
  "Creates a polyline."
  ([points]
   (polyline points {:stroke "#000000"}))

  ([points attrs]
   (create {:polyline (merge {:points (str/join " " (flatten points))} attrs)})))

(defn path
  "Creates a path"
  ([path-commands]
   (path path-commands {:stroke "#000000"}))

  ([path-commands attrs]
   (create {:path (merge {:d (str/join " " (flatten path-commands))} attrs)})))

(defn image
  "Creates an image"
  ([[x y] width height href]
   (image [x y] width height href nil))

  ([[x y] width height href attrs]
   (create {:image (merge {:x x :y y :width width :height height :href href} attrs)})))

(defn set-attribute
  "Sets the attribute of the selected elements."
  [key value]
  (rf/dispatch [:elements/set-attribute key value]))

(defn set-fill
  "Sets the fill color of the editor."
  [color]
  (rf/dispatch [:document/set-fill color]))

(defn set-stroke
  "Sets the stroke color of the editor."
  [color]
  (rf/dispatch [:document/set-stroke color]))

(defn db
  []
  @db/app-db)

(defn document
  []
  (get-in (db) [:documents (:active-document (db))]))

(defn elements
  []
  (:elements (document)))

(defn raise
  "Raises the selected elements."
  []
  (rf/dispatch [:elements/raise]))

(defn lower
  "Lowers the selected elements."
  []
  (rf/dispatch [:elements/lower]))

(defn group
  "Groups the selected elements."
  []
  (rf/dispatch [:elements/group]))

(defn ungroup
  "Ungroups the selected elements."
  []
  (rf/dispatch [:elements/ungroup]))

(defn select-all
  "Selects all elements."
  []
  (rf/dispatch [:elements/select-all]))

(defn deselect-all
  "Deselects all elements."
  []
  (rf/dispatch [:elements/deselect-all]))

(defn ->path
  "Converts selected elements to paths."
  []
  (rf/dispatch [:elements/->path]))

(defn stroke->path
  "Converts selected elements to paths."
  []
  (rf/dispatch [:elements/stroke->path]))

(defn align
  "Aligns the selected elements to the provided direction.
   Accepted directions
   :left :right :top :bottom :center-vertical :center-horizontal"
  [direction]
  (rf/dispatch [:elements/align direction]))

(defn al
  "Aligns the selected elements to the left."
  []
  (align :left))

(defn ar
  "Aligns the selected elements to the right"
  []
  (align :right))

(defn at
  "Aligns the selected elements to the top."
  [[]]
  (align :top))

(defn ab
  "Aligns the selected elements to the bottom."
  []
  (align :bottom))

(defn acv
  "Aligns the selected elements to the vertical center."
  []
  (align :center-vertical))

(defn ach
  "Aligns the selected elements to the horizontal center."
  []
  (align :center-horizontal))

(defn animate
  "Animates the selected elements."
  ([]
   (animate {}))

  ([attrs]
   (animate :animate attrs))

  ([tag attrs]
   (rf/dispatch [:elements/animate tag attrs])))

(defn undo
  "Goes back in history."
  ([]
   (undo 1))

  ([steps]
   (rf/dispatch [:history/undo steps])))

(defn redo
  "Goes forward in history."
  ([]
   (redo 1))

  ([steps]
   (rf/dispatch [:history/redo steps])))

(defn unite
  "Unites the selected elements."
  []
  (rf/dispatch [:elements/bool-operation :unite])
  "")

(defn intersect
  "Intersects the selected elements."
  []
  (rf/dispatch [:elements/bool-operation :intersect]))

(defn subtract
  "Subtracts the selected elements."
  []
  (rf/dispatch [:elements/bool-operation :subtract]))

(defn exclude
  "Excludes the selected elements."
  []
  (rf/dispatch [:elements/bool-operation :exclude]))

;; divide already refers to cljs.core/divide
(defn devide
  "Divides the selected elements."
  []
  (rf/dispatch [:elements/bool-operation :divide]))

(defn exit
  "Closes the application."
  [element]
  (apply #(rf/dispatch [:window/close]) element))

(defn help
  "Lists available functions."
  []
  (doseq [x (sort-by str (vals (ns-publics 'user)))]
    (print (:name (meta x))  " - " (:doc (meta x))))
  "")

(def version config/version)

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

(defn isometric-city []
  (let [grid-size 20
        building-colors ["#e9c46a" "#f4a261" "#e76f51"]
        building-shapes ["rect" "polygon"]
        buildings (repeatedly 100
                              #(hash-map :x (* grid-size (rand-int 20))
                                         :y (* grid-size (rand-int 20))
                                         :height (* grid-size (inc (rand-int 4)))
                                         :color (rand-nth building-colors)
                                         :shape (rand-nth building-shapes)))
        streets (concat (for [x (range 0 400 grid-size)]
                          (list [x 0] [x 400]))
                        (for [y (range 0 400 grid-size)]
                          (list [0 y] [400 y])))]
    (doseq [building buildings]
      (let [x (:x building)
            y (:y building)
            height (:height building)
            color (:color building)
            shape (:shape building)]
        (case shape
          "rect"
          (rect [x y] grid-size height {:fill color})
          "polygon"
          (let [points [[x y]
                        [x (+ y (* grid-size 0.5))]
                        [x (+ y (* grid-size 1.5))]
                        [x y]]
                top-points [[x y]
                            [x (+ y (* grid-size 0.5))]
                            [x (+ y (* grid-size 1.5))]
                            [x y]]
                bottom-points [[x y height]
                               [x (+ y (* grid-size 0.5)) height]
                               [x (+ y (* grid-size 1.5)) height]
                               [x y height]]]
            (doseq [p (partition 2 points)]
              (line (first p) (second p) {:stroke color :stroke-width 2}))
            (polygon top-points {:fill color})
            (polygon bottom-points {:fill color :opacity 0.5}))))
      (doseq [street streets]
        (line (first street) (second street) {:stroke "#222222"
                                              :stroke-dasharray "4,2"})))))
