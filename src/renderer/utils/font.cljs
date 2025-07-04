(ns renderer.utils.font
  (:require
   ["opentype.js" :as opentype]
   [clojure.string :as string]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.element :as utils.element]))

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
