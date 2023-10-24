(ns renderer.elements.effects
  (:require
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]
   [renderer.elements.handlers :as handlers]
   [renderer.history.handlers :as history]))


(defn elements->string
  [elements]
  (reduce #(str % (tools/render-to-string %2)) "" elements))

(rf/reg-fx
 :clipboard-write
 (fn [text-html]
   (js/navigator.clipboard.write
    (clj->js [(js/ClipboardItem.
               #js {"text/html" (when text-html
                                  (js/Blob.
                                   [text-html]
                                   #js {:type ["text/html"]}))})]))))

(rf/reg-event-fx
 :elements/copy
 (fn [{:keys [db]} [_]]
   (let [selected-elements (handlers/selected db)
         text-html (elements->string selected-elements)]
     {:db (handlers/copy db)
      :clipboard-write [text-html]})))

(rf/reg-event-fx
 :elements/cut
 (fn [{:keys [db]} [_]]
   (let [selected-elements (handlers/selected db)
         text-html (elements->string selected-elements)]
     {:db (-> db
              (handlers/copy)
              (handlers/delete)
              (history/finalize "Cut selection"))
      :clipboard-write [text-html]})))
