(ns benchmark-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [clojure.string :as str]
   [day8.re-frame.test :as rf-test]
   [malli.instrument :as mi]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]
   [renderer.document.events :as-alias document.e]
   [renderer.element.events :as-alias element.e]))

(defn bench
  "Returns the elapsed time of the event handling in milliseconds."
  ([e]
   (bench e 1))
  ([e iterations]
   (let [start (.getTime (js/Date.))
         _ (dotimes [_ iterations] (rf/dispatch e))
         end (.getTime (js/Date.))]
     (- end start))))

(deftest polygons
  (rf-test/run-test-sync
   (rf/dispatch [::app.e/initialize-db])
   (rf/dispatch [::document.e/init])

   ;; Istrumentation and db validation affects performance, so we disable it.
   (mi/unstrument!)
   (rf/clear-global-interceptor ::app.e/schema-validator)

   (testing "creating elements"
     (let [points (str/join " " (repeatedly 100 #(rand-int 1000)))]
       (is (> 1000 (bench [::element.e/add {:tag :polygon
                                            :attrs {:points points}}] 20)))))

   (testing "selecting elements"
     (is (> 1000 (bench [::element.e/select-all]))))

   (testing "deselecting elements"
     (is (> 1000 (bench [::element.e/deselect-all]))))

   (testing "moving elements"
     (rf/dispatch [::element.e/select-all])
     (is (> 100 (bench [::element.e/translate [100 100]]))))

   (testing "scaling elements"
     (is (> 100 (bench [::element.e/scale [100 100]]))))

   (mi/instrument!)
   (rf/reg-global-interceptor app.e/schema-validator)))
