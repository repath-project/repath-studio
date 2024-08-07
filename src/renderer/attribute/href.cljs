(ns renderer.attribute.href
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/href"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.components :as comp]
   [renderer.element.events :as-alias element.e]
   [renderer.utils.file :as file]))

(defmethod hierarchy/description :href
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

(defmethod hierarchy/form-element :href
  [k v disabled?]
  (let [state-default? (= @(rf/subscribe [:state]) :default)]
    [:<>
     [v/form-input
      {:key k
       :value (if state-default? v "waiting")
       :disabled? (or disabled?
                      (not v)
                      (not state-default?))}]
     [:button.button.ml-px.inline-block.bg-primary.text-muted
      {:title "Select file"
       :style {:flex "0 0 26px"
               :height "100%"}
       :on-click #(file/open!
                   {:startIn "pictures"
                    :types [{:accept {"image/png" [".png"]
                                      "image/jpeg" [".jpeg" ".jpg"]
                                      "image/bmp" [".fmp"]}}]}
                   (fn [file]
                     (rf/dispatch [:set-tool :select])
                     (update-href! file)))}
      [comp/icon "folder"]]]))
