(ns renderer.tool.text
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.events :as-alias element.e]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.tool.base :as tool]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.units :as units]))

(derive :text ::tool/shape)

(defmethod tool/properties :text
  []
  {:icon "text"
   :description "The SVG <text> element draws a graphics element consisting
                 of text. It's possible to apply a gradient, pattern,
                 clipping path, mask, or filter to <text>, like any other SVG
                 graphics element."
   :attrs [:font-family
           :font-size
           :font-weight
           :font-style
           :fill
           :stroke
           :stroke-width
           :opacity]})

(defmethod tool/activate :text
  [db]
  (-> db
      (assoc :cursor "text")
      (h/set-message
       [:div
        [:div "Click to enter your text."]])))

(defmethod tool/pointer-up :text
  [{:keys [adjusted-pointer-offset] :as db}]
  (let [[offset-x offset-y] adjusted-pointer-offset
        attrs {:x offset-x
               :y offset-y}]
    (-> db
        element.h/deselect
        (element.h/add {:type :element
                        :tag :text
                        :attrs attrs})
        (history.h/finalize "Create text")
        (h/set-tool :edit)
        (h/set-state :edit)))) ; FIXME: Merge create and edit history action.

(defmethod tool/drag-end :text
  [db e]
  (tool/pointer-up db e))

(defmethod tool/translate :text
  [el [x y]]
  (-> el
      (attr.hierarchy/update-attr :x + x)
      (attr.hierarchy/update-attr :y + y)))

(defmethod tool/scale :text
  [el ratio pivot-point]
  (let [offset (mat/sub pivot-point (mat/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :font-size * ratio)
        (tool/translate offset))))

(defn get-text
  [e]
  (str/replace (.. e -target -value) " " "\u00a0")) ; REVIEW

(defn set-text-and-select-element
  [e el-k]
  (let [s (get-text e)]
    (rf/dispatch (if (empty? s)
                   [::element.e/delete]
                   [::element.e/set-prop el-k :content s]))
    (rf/dispatch [:set-tool :select])))

(defn key-down-handler
  [e el-k]
  (.stopPropagation e)
  (if (contains? #{"Enter" "Escape"} (.-code e))
    (set-text-and-select-element e el-k)
    (.requestAnimationFrame
     js/window
     #(rf/dispatch-sync [::element.e/preview-prop el-k :content (get-text e)]))))

(defmethod tool/render-edit :text
  [{:keys [attrs key content] :as el}]
  (let [offset (element/offset el)
        el-bounds (tool/bounds el)
        [x y] (mat/add (take 2 el-bounds) offset)
        [width height] (bounds/->dimensions el-bounds)
        {:keys [fill font-family font-size font-weight]} attrs]
    [:foreignObject {:x x
                     :y y
                     :width (+ width 20)
                     :height height}
     [:input
      {:key key
       :default-value content
       :auto-focus true
       :on-focus #(.. % -target select)
       :on-pointer-down #(.stopPropagation %)
       :on-pointer-up #(.stopPropagation %)
       :on-blur #(set-text-and-select-element % key)
       :on-key-down #(key-down-handler % key)
       :style {:color "transparent"
               :caret-color (or fill "black")
               :display "block"
               :width (+ width 15)
               :height height
               :padding 0
               :border 0
               :outline "none"
               :background "transparent"
               :font-family (if (empty? font-family) "inherit" font-family)
               :font-size (if (empty? font-size)
                            "inherit"
                            (str (units/unit->px font-size) "px"))
               :font-weight (if (empty? font-weight) "inherit" font-weight)}}]]))

(defmethod tool/path :text
  [{:keys [attrs content]}]
  (let [font-descriptor #js {:family (:font-family attrs)
                             :weight (js/parseInt (:font-weight attrs))
                             :italic (= (:font-style attrs) "italic")}]
    (.textToPath
     js/window.api
     content
     #js {:font-url (.-path (.findFont js/window.api font-descriptor))
          :x (js/parseFloat (:x attrs))
          :y (js/parseFloat (:y attrs))
          :font-size (js/parseFloat (or (:font-size attrs) 16))}))) ; FIXME
