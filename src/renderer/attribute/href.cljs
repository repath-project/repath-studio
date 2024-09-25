(ns renderer.attribute.href
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/href"
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.app.subs :as-alias app.s]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.element.events :as-alias element.e]
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
  (let [state-default? (= @(rf/subscribe [::app.s/state]) :default)
        data-url? (str/starts-with? v "data:")]
    [:div.flex.gap-px
     [v/form-input k (if data-url? "data-url" v)
      {:disabled (or disabled
                     data-url?
                     (not v)
                     (not state-default?))}]
     [:button.button.inline-block.bg-primary.text-muted
      {:title "Select file"
       :disabled disabled
       :style {:flex "0 0 26px"
               :height "100%"}
       :on-click #(file/open!
                   {:startIn "pictures"
                    :types [{:accept {"image/png" [".png"]
                                      "image/jpeg" [".jpeg" ".jpg"]
                                      "image/bmp" [".fmp"]}}]}
                   (fn [file]
                     (rf/dispatch [::app.e/set-tool :select])
                     (update-href! file)))}
      [ui/icon "folder"]]]))
