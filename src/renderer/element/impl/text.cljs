(ns renderer.element.impl.text
  "https://www.w3.org/TR/SVG/text.html
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/text"
  (:require
   ["opentype.js" :as opentype]
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
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
   [renderer.utils.length :as utils.length]))

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
       :on-blur #(rf/dispatch [::set-text id (get-text! %)])
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

(rf/reg-event-db
 ::->path
 (fn [db [_ id d]]
   (element.handlers/update-el db id utils.element/->path d)))

;; https://drafts.csswg.org/css-fonts/#absolute-size-mapping
(defonce size-scale-factor
  {"xx-small" (/ 3 5)
   "x-small" (/ 3 4)
   "small" (/ 8 9)
   "medium" 1
   "large" (/ 6 5)
   "x-large" (/ 3 2)
   "xx-large" (/ 2 1)
   "xxx-large" (/ 3 1)})

(defn font-size->px
  [font-size]
  (let [scale-factor (get size-scale-factor font-size)]
    (if font-size
      (if scale-factor
        (str (* scale-factor 16))
        font-size)
      "16")))

(defmethod element.hierarchy/path :text
  [el]
  (let [{:keys [attrs content id]} el
        {:keys [x y font-size weight font-style font-family]} attrs
        system-fonts @(rf/subscribe [::app.subs/system-fonts])
        family-name (or font-family "Adwaita Sans")
        _style-name (or font-style "Normal")
        font-size (font-size->px font-size)
        [x y font-size] (mapv utils.length/unit->px [x y font-size weight])
        postscript-name (-> system-fonts (get family-name) :postscript-name)]
    (-> (js/window.queryLocalFonts #js {:postscriptNames [postscript-name]})
        (.then (fn [fonts]
                 (print "Fonts found: " (count fonts))
                 (when-let [font (first fonts)]
                   (-> (.blob font)
                       (.then (fn [blob]
                                (-> (.arrayBuffer blob)
                                    (.then (fn [buffer]
                                             (let [opentype-font (opentype/parse buffer)
                                                   path (.getPath opentype-font content
                                                                  x y font-size)
                                                   d (.toPathData path)]
                                               (rf/dispatch [::->path id d])))))))))))
        (.catch (fn [_error])))))
