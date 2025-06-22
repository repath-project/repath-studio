(ns renderer.element.impl.text
  "https://www.w3.org/TR/SVG/text.html
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/text"
  (:require
   ["opentype.js" :as opentype]
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
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.dom :as utils.dom]
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
  (let [bounds (element.hierarchy/bbox el)
        [_w h] (utils.bounds/->dimensions bounds)
        offset (utils.element/scale-offset ratio (matrix/sub pivot-point [0 (/ h 2)]))
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
             (element.handlers/refresh-bbox id)
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
        {:keys [fill font-family font-size font-weight font-style]} attrs
        font-size-px (utils.length/unit->px font-size)
        font-size (if (zero? font-size-px) font-size (str font-size-px "px"))]
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
               :font-family font-family
               :font-size font-size
               :font-weight font-weight}}]]))

(defn get-computed-styles!
  [{:keys [content] :as el}]
  (when-let [svg (utils.dom/canvas-element!)]
    (let [dom-el (utils.element/->dom-element el)]
      (.appendChild svg dom-el)
      (set! (.-innerHTML dom-el) (if (empty? content) "\u00a0" content))
      (let [computed-style (.getComputedStyle js/window dom-el nil)
            font-style (.getPropertyValue computed-style "font-style")
            font-size (.getPropertyValue computed-style "font-size")
            font-weight (.getPropertyValue computed-style "font-weight")
            bbox (utils.bounds/dom-el->bbox dom-el)]
        (.remove dom-el)
        {:font-style font-style
         :font-size font-size
         :font-weight font-weight
         :bbox bbox}))))

(defn font-file->path-data
  [file content x y font-size]
  (-> (.blob file)
      (.then (fn [blob]
               (-> (.arrayBuffer blob)
                   (.then (fn [buffer]
                            (let [font (opentype/parse buffer)
                                  path (.getPath font content x y font-size)]
                              (.toPathData path)))))))))

(defn includes-prop?
  [v prop]
  (when v
    (string/includes? (string/lower-case v) (string/lower-case prop))))

(defn match-font-by-weight
  [weight fonts]
  (let [weight-num (js/parseInt weight)
        weight-names (get utils.attribute/weight-name-mapping weight)
        includes-weight? (fn [font]
                           (some #(includes-prop? % (.-style font)) weight-names))
        matched-weight (filter includes-weight? fonts)]
    (if (or (seq matched-weight) (< weight-num 100))
      matched-weight
      (recur (str (- weight-num 100)) fonts))))

(defn match-font
  [fonts family style weight]
  (let [matched-family (filter #(includes-prop? family (.-family %)) fonts)
        matched-style (filter #(includes-prop? style (.-style %)) matched-family)
        matched-weight (match-font-by-weight weight (if (seq matched-style)
                                                      matched-style
                                                      matched-family))]
    (or (first matched-weight)
        (first matched-style)
        (first matched-family)
        (first fonts))))

(defn default-font-path
  [font-style font-weight]
  (str "./css/files/noto-sans-latin-" font-weight "-" font-style ".woff"))

(defmethod element.hierarchy/path :text
  [el]
  (let [{:keys [attrs content]} el
        {:keys [x y font-family]} attrs
        {:keys [font-size font-style font-weight]} (get-computed-styles! el)
        [x y font-size] (mapv utils.length/unit->px [x y font-size])]
    (if font-family
      (-> (js/window.queryLocalFonts)
          (.then (fn [fonts]
                   (when-let [font (match-font fonts
                                               font-family
                                               font-style
                                               font-weight)]
                     (font-file->path-data font content x y font-size)))))
      (-> (js/fetch (default-font-path font-style font-weight))
          (.then (fn [response]
                   (font-file->path-data response content x y font-size)))))))

(defmethod element.hierarchy/bbox :text
  [el]
  (:bbox (get-computed-styles! el)))
