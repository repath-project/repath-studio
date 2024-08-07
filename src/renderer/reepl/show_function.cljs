(ns renderer.reepl.show-function
  (:require
   [clojure.string :as str]))

(def cljs-fn-prefix
  "cljs$core$IFn$_invoke$arity$")

(defn recover-cljs-name
  [parts]
  (-> (str/join \. (butlast parts))
      (str \/ (last parts))
      demunge))

(defn get-cljs-arities
  [fn]
  (map
   #(aget fn %)
   (filter #(.startsWith % cljs-fn-prefix) (js->clj (js/Object.keys fn)))))

(defn get-fn-summary
  [fn]
  (let [source (str fn)
        args (second (re-find #"\(([^\)]+)\)" source))]
    (map demunge
         (str/split args \,))))

(defn get-function-forms
  [fn]
  (let [arities (get-cljs-arities fn)
        arities (if (empty? arities)
                  [fn]
                  arities)]
    (map get-fn-summary
         arities)))

(defn get-fn-name
  [fn]
  (let [parts (.split (.-name fn) \$)]
    (cond
      (empty? (.-name fn)) "*anonymous*"
      (= 1 (count parts)) (.-name fn)
      :else (recover-cljs-name parts))))

(defn str-fn-forms
  [forms]
  (str \[ (str/join "] [" (map (partial str/join " ") forms)) \]))

(defn show-fn-with-docs
  [get-doc fn _ _]
  (when (= js/Function (type fn))
    (let [docs (get-doc (symbol (get-fn-name fn)))
          is-native-fn (.match (str fn) #"\{ \[native code\] \}$")]
      [:div.inline-block.shrink-0.box-border
       (if docs
         [:span.function-docs docs]
         [:<>
          [:span.function-head "fn " (get-fn-name fn)]
          [:span.function-arities (str-fn-forms (get-function-forms fn))]
          [:span.function-body (when is-native-fn "[native code]")]])])))
