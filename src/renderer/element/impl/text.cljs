(ns renderer.element.impl.text
  "https://www.w3.org/TR/SVG/text.html
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/text"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.event.impl.keyboard :as event.impl.keyboard]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.events :as tool.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.subs :as-alias tool.subs]
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
  (let [offset (utils.element/scale-offset ratio pivot-point)
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :font-size * ratio)
        (element.hierarchy/translate offset))))

(defn get-text!
  "Retrieves the input value and replaces spaces with no-break space to maintain
   user intent."
  [e]
  (string/replace (.. e -target -value) " " "\u00a0"))

(rf/reg-event-db
 ::set-text
 (fn [db [_ id s]]
   (-> (if (empty? s)
         (-> (element.handlers/delete db id)
             (history.handlers/finalize "Remove text"))
         (-> (element.handlers/assoc-prop db id :content s)
             (history.handlers/finalize "Set text")))
       (tool.handlers/activate :transform))))

(defmethod element.hierarchy/render :text
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        state @(rf/subscribe [::tool.subs/state])
        tool @(rf/subscribe [::tool.subs/active])]
    (when-not (and (= tool :edit) (:selected el))
      [element.views/render-to-dom el child-els (= state :idle)])))

(defmethod element.hierarchy/render-edit :text
  [el]
  (let [{:keys [id content]} el
        offset (utils.element/offset el)
        el-bbox (element.hierarchy/bbox el)
        [x y] (matrix/add (take 2 el-bbox) offset)
        [_w h] (utils.bounds/->dimensions el-bbox)
        attrs (utils.element/attributes el)
        {:keys [fill font-family font-size font-weight font-style]} attrs]
    [:foreignObject {:x x
                     :y y
                     :width "1000vw"
                     :height h}
     [:input
      {:key id
       :default-value content
       :auto-focus true
       :on-focus #(.. % -target select)
       :on-pointer-down #(.stopPropagation %)
       :on-pointer-up #(.stopPropagation %)
       :on-blur #(rf/dispatch-sync [::set-text id (get-text! %)])
       :on-key-down #(event.impl.keyboard/input-key-down-handler! % content identity id)
       :ref (fn [this] (when this (rf/dispatch [::tool.events/set-state :type])))
       :style {:color fill
               :caret-color fill
               :display "block"
               :width "1000vw"
               :height h
               :padding 0
               :border 0
               :outline "none"
               :background "transparent"
               :font-style font-style
               :font-family (if (empty? font-family) "inherit" font-family)
               :font-size (if (empty? font-size)
                            "inherit"
                            (str (utils.length/unit->px font-size) "px"))
               :font-weight (if (empty? font-weight) "inherit" font-weight)}}]]))

(when utils.system/electron?
  (defmethod element.hierarchy/path :text
    [el]
    (let [{:keys [attrs content]} el
          font-descriptor #js {:family (:font-family attrs)
                               :weight (js/parseInt (:font-weight attrs))
                               :italic (= (:font-style attrs) "italic")}]
      (.textToPath
       js/window.api
       content
       #js {:font-url (.-path (.findFont js/window.api font-descriptor))
            :x (js/parseFloat (:x attrs))
            :y (js/parseFloat (:y attrs))
            :font-size (js/parseFloat (or (:font-size attrs) 16))})))) ; FIXME
