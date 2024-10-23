(ns renderer.attribute.impl.href
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/href"
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.element.events :as-alias element.e]
   [renderer.tool.events :as-alias tool.e]
   [renderer.tool.subs :as-alias tool.s]
   [renderer.ui :as ui]
   [renderer.utils.file :as file]))

(defmethod hierarchy/description [:default :href]
  []
  "The href attribute defines a link to a resource as a reference URL.
   The exact meaning of that link depends on the context of each element using it.")

(defn update-href!
  [^js/File file]
  (let [reader (js/FileReader.)]
    (.addEventListener
     reader
     "load"
     #(rf/dispatch [::element.e/set-attr :href (.-result reader)]))
    (.readAsDataURL reader file)))

(defmethod hierarchy/form-element [:default :href]
  [_ k v {:keys [disabled]}]
  (let [state-default? (= @(rf/subscribe [::tool.s/state]) :idle)
        data-url? (str/starts-with? v "data:")]
    [:div.flex.gap-px.w-full
     [v/form-input k (if data-url? "data-url" v)
      {:disabled (or disabled
                     data-url?
                     (not v)
                     (not state-default?))}]
     [:button.form-control-button
      {:title "Select file"
       :disabled disabled
       :on-click #(file/open!
                   {:options {:startIn "pictures"
                              :types [{:accept {"image/png" [".png"]
                                                "image/jpeg" [".jpeg" ".jpg"]
                                                "image/bmp" [".fmp"]}}]}
                    :callback (fn [file]
                                (rf/dispatch [::tool.e/activate :transform])
                                (update-href! file))})}
      [ui/icon "folder"]]]))
