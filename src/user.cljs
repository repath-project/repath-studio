(ns user
  (:require
   [clojure.math]
   [clojure.string :as string]
   [config :as config]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [renderer.a11y.events :as-alias a11y.events]
   [renderer.document.events :as-alias document.events]
   [renderer.element.events :as-alias element.events]
   [renderer.history.events :as-alias history.events]
   [renderer.window.events :as-alias window.events]))

(defn ^:export translate
  "Moves the selected elements."
  ([offset]
   (rf/dispatch [::element.events/translate offset]))
  ([x y]
   (translate [x y])))

(defn ^:export place
  "Moves the selected elements."
  ([pos]
   (rf/dispatch [::element.events/place pos]))
  ([x y]
   (place [x y])))

(defn ^:export scale
  "Scales the selected elements."
  ([ratio]
   (rf/dispatch [::element.events/scale (if (number? ratio)
                                          [ratio ratio]
                                          ratio)]))
  ([x y]
   (rf/dispatch [::element.events/scale [x y]])))

(defn ^:export fill
  "Fills the selected elements."
  [color]
  (rf/dispatch [::element.events/set-attr :fill color]))

(defn ^:export delete
  "Deletes the selected elements."
  []
  (rf/dispatch [::element.events/delete]))

(defn ^:export copy
  "Copies the selected elements."
  []
  (rf/dispatch [::element.events/copy]))

(defn ^:export paste
  "Pastes the selected elements."
  []
  (rf/dispatch [::element.events/paste]))

(defn ^:export paste-in-place
  "Pastes the selected elements in place."
  []
  (rf/dispatch [::element.events/paste-in-place]))

(defn ^:export duplicate
  "Duplicates the selected elements."
  []
  (rf/dispatch [::element.events/duplicate]))

(defn ^:export create
  "Creates a new element."
  [el]
  (rf/dispatch [::element.events/add el]))

(defn ^:export circle
  "Creates a circle."
  ([[cx cy] r]
   (circle [cx cy] r nil))

  ([[cx cy] r attrs]
   (create {:tag :circle
            :attrs (merge {:cx cx
                           :cy cy
                           :r r} attrs)})))

(defn ^:export rect
  "Creates a rectangle."
  ([[x y] width height]
   (rect [x y] width height nil))

  ([[x y] width height attrs]
   (create {:tag :rect
            :attrs (merge {:x x
                           :y y
                           :width width
                           :height height} attrs)})))

(defn ^:export line
  "Creates a line."
  ([[[x1 y1] [x2 y2]]]
   (line [x1 y1] [x2 y2] {:stroke "#000000"}))
  ([[x1 y1] [x2 y2]]
   (line [x1 y1] [x2 y2] {:stroke "#000000"}))
  ([[x1 y1] [x2 y2] attrs]
   (create {:tag :line
            :attrs (merge {:x1 x1
                           :y1 y1
                           :x2 x2
                           :y2 y2} attrs)})))

(defn ^:export polygon
  "Creates a polygon."
  ([points]
   (polygon points {:stroke "#000000"}))
  ([points attrs]
   (create {:tag :polygon
            :attrs (merge {:points (string/join " " (flatten points))}
                          attrs)})))

(defn ^:export polyline
  "Creates a polyline."
  ([points]
   (polyline points {:stroke "#000000"}))
  ([points attrs]
   (create {:tag :polyline
            :attrs (merge {:points (string/join " " (flatten points))}
                          attrs)})))

(defn ^:export path
  "Creates a path."
  ([path-commands]
   (path path-commands {:stroke "#000000"}))
  ([path-commands attrs]
   (create {:path (merge {:d (string/join " " (flatten path-commands))}
                         attrs)})))

(defn ^:export image
  "Creates an image."
  ([[x y] width height href]
   (image [x y] width height href nil))
  ([[x y] width height href attrs]
   (create {:tag :image
            :attrs (merge {:x x
                           :y y
                           :width width
                           :height height
                           :href href} attrs)})))

(defn ^:export text
  "Creates a text element."
  ([[x y] content]
   (text [x y] content nil))
  ([[x y] content attrs]
   (create {:tag :text
            :content content
            :attrs (merge {:x x
                           :y y} attrs)})))

(defn ^:export set-attr
  "Sets the attribute of the selected elements."
  [k v]
  (rf/dispatch [::element.events/set-attr k v]))

(defn ^:export set-fill
  "Sets the fill color of the editor."
  [color]
  (rf/dispatch [::document.events/set-attr :fill color]))

(defn ^:export set-stroke
  "Sets the stroke color of the editor."
  [color]
  (rf/dispatch [::document.events/set-attr :stroke color]))

(defn ^:export db
  []
  @rf.db/app-db)

(defn ^:export document
  []
  (get-in (db) [:documents (:active-document (db))]))

(defn ^:export elements
  []
  (:elements (document)))

(defn ^:export raise
  "Raises the selected elements."
  []
  (rf/dispatch [::element.events/raise]))

(defn ^:export lower
  "Lowers the selected elements."
  []
  (rf/dispatch [::element.events/lower]))

(defn ^:export group
  "Groups the selected elements."
  []
  (rf/dispatch [::element.events/group]))

(defn ^:export ungroup
  "Ungroups the selected elements."
  []
  (rf/dispatch [::element.events/ungroup]))

(defn ^:export select-all
  "Selects all elements."
  []
  (rf/dispatch [::element.events/select-all]))

(defn ^:export deselect-all
  "Deselects all elements."
  []
  (rf/dispatch [::element.events/deselect-all]))

(defn ^:export ->path
  "Converts the selected elements to paths."
  []
  (rf/dispatch [::element.events/->path]))

(defn ^:export stroke->path
  "Converts the selected elements' stroke to paths."
  []
  (rf/dispatch [::element.events/stroke->path]))

(defn ^:export align
  "Aligns the selected elements to the provided direction.
   Accepted directions
   :left :right :top :bottom :center-vertical :center-horizontal"
  [direction]
  (rf/dispatch [::element.events/align direction]))

(defn ^:export al
  "Aligns the selected elements to the left."
  []
  (align :left))

(defn ^:export ar
  "Aligns the selected elements to the right."
  []
  (align :right))

(defn ^:export at
  "Aligns the selected elements to the top."
  []
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
   (rf/dispatch [::element.events/animate tag attrs])))

(defn ^:export undo
  "Goes back in history."
  ([]
   (rf/dispatch [::history.events/undo]))
  ([steps]
   (rf/dispatch [::history.events/undo-by steps])))

(defn ^:export redo
  "Goes forward in history."
  ([]
   (rf/dispatch [::history.events/redo]))
  ([steps]
   (rf/dispatch [::history.events/redo-by steps])))

(defn ^:export unite
  "Unites the selected elements."
  []
  (rf/dispatch [::element.events/boolean-operation :unite]))

(defn ^:export intersect
  "Intersects the selected elements."
  []
  (rf/dispatch [::element.events/boolean-operation :intersect]))

(defn ^:export subtract
  "Subtracts the selected elements."
  []
  (rf/dispatch [::element.events/boolean-operation :subtract]))

(defn ^:export exclude
  "Excludes the selected elements."
  []
  (rf/dispatch [::element.events/boolean-operation :exclude]))

(defn ^:export div
  "Divides the selected elements."
  []
  (rf/dispatch [::element.events/boolean-operation :divide]))

(defn ^:export exit
  "Closes the application."
  []
  (rf/dispatch [::window.events/close]))

(defn ^:export register-a11y-filter
  "Registers an accessibility filter."
  [v]
  (rf/dispatch [::a11y.events/register-filter v]))

(defn ^:export deregister-a11y-filter
  "Deregisters an accessibility filter."
  [v]
  (rf/dispatch [::a11y.events/deregister-filter v]))

(defn ^:export help
  "Lists available functions."
  []
  (doseq [x (sort-by str (vals (ns-publics 'user)))]
    (print (:name (meta x)) " - " (:doc (meta x))))
  "")

;; Expose all commands to global namespace.
(doseq [command (vals (ns-publics 'user))]
  (aset js/window (:name (meta command)) (.call ^js (.-val command))))

(def ^:export version config/version)

(comment
  (dotimes [x 25]
    (circle [(+ (* x 30) 40) (+ (* (js/Math.sin x) 10) 200)]
            10
            {:fill (str "hsl(" (* x 10) " ,50% , 50%)")}))
  #())
