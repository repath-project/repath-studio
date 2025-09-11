(ns renderer.utils.font
  (:require
   ["opentype.js" :as opentype]
   [clojure.string :as string]
   [malli.core :as m]
   [renderer.app.db :refer [SystemFonts]]
   [renderer.element.db :refer [Element]]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.element :as utils.element]))

(m/=> get-computed-styles! [:-> Element [:maybe map?]])
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

(m/=> font-data->path-data! [:-> any? string? number? number? number? [:maybe string?]])
(defn font-data->path-data!
  [^js/FontData font-data text x y font-size]
  (-> (.blob font-data)
      (.then (fn [^js/Blob blob]
               (-> (.arrayBuffer blob)
                   (.then (fn [^js/ArrayBuffer buffer]
                            (let [^js/Font font (opentype/parse buffer)
                                  ^js/Path path (.getPath font text x y font-size)]
                              (.toPathData path)))))))))

(m/=> includes-prop? [:-> string? string? boolean?])
(defn includes-prop?
  [v prop]
  (when v
    (string/includes? (string/lower-case v) (string/lower-case prop))))

(m/=> match-font-by-weight [:-> string? any? string?])
(defn match-font-by-weight
  [weight ^js/Array fonts]
  (let [weight-num (js/parseInt weight)
        weight-names (get utils.attribute/weight-name-mapping weight)
        includes-weight? (fn [font]
                           (some #(includes-prop? % (.-style font)) weight-names))
        matched-weight (filter includes-weight? fonts)]
    (if (or (seq matched-weight) (< weight-num 100))
      matched-weight
      (recur (str (- weight-num 100)) fonts))))

(m/=> match-font [:-> any? string? string? string? any?])
(defn match-font
  [^js/Array fonts family style weight]
  (let [matched-family (filter #(includes-prop? family (.-family %)) fonts)
        matched-style (filter #(includes-prop? style (.-style %)) matched-family)
        matched-weight (match-font-by-weight weight (if (seq matched-style)
                                                      matched-style
                                                      matched-family))]
    (or (first matched-weight)
        (first matched-style)
        (first matched-family)
        (first fonts))))

(m/=> default-font-path [:-> string? string? string?])
(defn default-font-path
  [font-style font-weight]
  (str "./css/files/noto-sans-latin-" font-weight "-" font-style ".woff"))

(m/=> font-data->fonts [:-> any? SystemFonts])
(defn font-data->system-fonts
  [^js/Array available-fonts]
  (->> available-fonts
       (reduce (fn [fonts ^js/FontData font-data]
                 (let [family (.-family font-data)
                       style (.-style font-data)]
                   (assoc-in fonts [family style]
                             {:postscript-name (.-postscriptName font-data)
                              :full-name (.-fullName font-data)}))) {})))
