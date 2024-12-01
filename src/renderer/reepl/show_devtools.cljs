(ns renderer.reepl.show-devtools
  (:require
   [clojure.string :as str]
   [devtools.formatters.core :as devtools]
   [reagent.core :as r]))

(defn js-array?
  [v]
  (= js/Array (type v)))

(defn parse-style
  [raw]
  (into {}
        (map (fn [line]
               (let [[k v] (str/split line ":")]
                 [(keyword k) v])) (str/split raw ";"))))

(defn show-el
  [v show-value]
  (let [opts (second v)
        children (drop 2 v)]
    (if (= "object" (first v))
      [show-value (.-object opts) (.-config opts)]
      (into
       [(keyword (first v)) {:style (when opts (parse-style (.-style opts)))}]
       (map #(if-not (js-array? %) % (show-el % show-value)) children)))))

(defn openable
  [header v config show-value]
  (let [open (r/atom false)]
    (fn [_ _]
      (let [open? @open]
        [:div.flex.flex-col
         [:div.flex
          [:div.flex.cursor-pointer.px-1
           {:on-click #(swap! open not)}
           (if open? "▼" "▶")]
          (show-el header show-value)]
         (when open?
           (show-el (devtools/body-api-call v config) show-value))]))))

;; see https://docs.google.com/document/d/1FTascZXT9cxfetuPRT2eXPQKXui4nWFivUnS_335T3U/preview
(defn show-devtools
  [v config show-value]
  (when-not (var? v)
    (let [header (try
                   (devtools/header-api-call v config)
                   (catch js/Error e
                     e))]
      (cond
        (not header)
        nil

        (instance? js/Error header)
        [:div.inline-flex.text-error "Error expanding lazy value"]

        :else
        (if-not (devtools/has-body-api-call v config)
          [:div.inline-flex.devtools (show-el header show-value)]
          [openable header v config show-value])))))
