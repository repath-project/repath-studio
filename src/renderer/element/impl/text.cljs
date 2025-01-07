(ns renderer.element.impl.text
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.app.events :as-alias app.e]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.events :as-alias element.e]
   [renderer.element.handlers :as element.h]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.history.handlers :as history.h]
   [renderer.tool.handlers :as h]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.length :as length]
   [renderer.utils.system :as system]))

(derive :text ::hierarchy/shape)

(defmethod hierarchy/properties :text
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

(defmethod hierarchy/translate :text
  [el [x y]]
  (element/update-attrs-with el + [[:x x] [:y y]]))

(defmethod hierarchy/scale :text
  [el ratio pivot-point]
  (let [offset (mat/sub pivot-point (mat/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :font-size * ratio)
        (hierarchy/translate offset))))

(defn get-text!
  "Retrieves the input value and replaces spaces with no-break space to maintain
   user intent."
  [e]
  (str/replace (.. e -target -value) " " "\u00a0"))

(rf/reg-event-fx
 ::set-text
 (fn [{:keys [db]} [_ id s]]
   {:db (-> (if (empty? s)
              (-> (element.h/delete db id)
                  (history.h/finalize "Remove text"))
              (-> (element.h/assoc-prop db id :content s)
                  (history.h/finalize "Set text")))
            (h/activate :transform))
    ::app.fx/focus nil}))

(defn key-down-handler!
  [e id]
  (.stopPropagation e)
  (.requestAnimationFrame
   js/window
   #(rf/dispatch-sync (if (contains? #{"Enter" "Escape"} (.-code e))
                        [::set-text id (get-text! e)]
                        [::element.e/preview-prop id :content (get-text! e)]))))

(defmethod hierarchy/render-edit :text
  [el]
  (let [{:keys [attrs id content]} el
        offset (element/offset el)
        el-bbox (hierarchy/bbox el)
        [x y] (mat/add (take 2 el-bbox) offset)
        [w h] (bounds/->dimensions el-bbox)
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
                            (str (length/unit->px font-size) "px"))
               :font-weight (if (empty? font-weight) "inherit" font-weight)}}]]))

(when system/electron?
  (defmethod hierarchy/path :text
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
