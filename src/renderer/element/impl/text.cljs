(ns renderer.element.impl.text
  "https://www.w3.org/TR/SVG/text.html
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/text"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.events :as-alias element.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]
   [renderer.utils.system :as utils.system]))

(derive :text ::element.hierarchy/shape)

(defmethod element.hierarchy/properties :text
  []
  {:icon "text"
   :description "The SVG <text> element draws a graphics element consisting
                 of text. It's possible to apply a gradient, pattern,
                 clipping path, mask, or filter to <text>, like any other SVG
                 graphics element."
   :ratio-locked true
   :attrs [:font-family
           :font-size
           :font-weight
           :font-style
           :fill
           :stroke
           :stroke-width
           :opacity]})

(defmethod element.hierarchy/translate :text
  [el [x y]]
  (utils.element/update-attrs-with el + [[:x x] [:y y]]))

(defmethod element.hierarchy/scale :text
  [el ratio pivot-point]
  (let [offset (matrix/sub pivot-point (matrix/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :font-size * ratio)
        (element.hierarchy/translate offset))))

(defn get-text!
  "Retrieves the input value and replaces spaces with no-break space to maintain
   user intent."
  [e]
  (string/replace (.. e -target -value) " " "\u00a0"))

(rf/reg-event-fx
 ::set-text
 (fn [{:keys [db]} [_ id s]]
   {:db (-> (if (empty? s)
              (-> (element.handlers/delete db id)
                  (history.handlers/finalize "Remove text"))
              (-> (element.handlers/assoc-prop db id :content s)
                  (history.handlers/finalize "Set text")))
            (tool.handlers/activate :transform))
    ::app.effects/focus nil}))

(defn key-down-handler!
  [e id]
  (.stopPropagation e)
  (.requestAnimationFrame
   js/window
   #(rf/dispatch-sync (if (contains? #{"Enter" "Escape"} (.-code e))
                        [::set-text id (get-text! e)]
                        [::element.events/preview-prop id :content (get-text! e)]))))

(defmethod element.hierarchy/render-edit :text
  [el]
  (let [{:keys [attrs id content]} el
        offset (utils.element/offset el)
        el-bbox (element.hierarchy/bbox el)
        [x y] (matrix/add (take 2 el-bbox) offset)
        [w h] (utils.bounds/->dimensions el-bbox)
        {:keys [fill font-family font-size font-weight]} attrs]
    [:foreignObject {:x x
                     :y y
                     :width (+ w 20)
                     :height h}
     [:input
      {:key id
       :default-value content
       :auto-focus true
       :on-focus #(.. % -target select)
       :on-pointer-down #(.stopPropagation %)
       :on-pointer-up #(.stopPropagation %)
       :on-blur #(rf/dispatch [::set-text id (get-text! %)])
       :on-key-down #(key-down-handler! % id)
       :style {:color "transparent"
               :caret-color (or fill "black")
               :display "block"
               :width (+ w 15)
               :height h
               :padding 0
               :border 0
               :outline "none"
               :background "transparent"
               :font-family (if (empty? font-family) "inherit" font-family)
               :font-size (if (empty? font-size)
                            "inherit"
                            (str (utils.length/unit->px font-size) "px"))
               :font-weight (if (empty? font-weight) "inherit" font-weight)}}]]))

(when utils.system/electron?
  (defmethod element.hierarchy/path :text
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
            :font-size (js/parseFloat (or (:font-size attrs) 16))})))) ; FIXME
