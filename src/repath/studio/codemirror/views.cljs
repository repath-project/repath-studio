(ns repath.studio.codemirror.views
  (:require [reagent.core :as ra]
            [reagent.dom :as dom]
            ["codemirror" :as codemirror]
            ["codemirror/mode/css/css.js"]
            ["codemirror/addon/hint/show-hint.js"]
            ["codemirror/addon/hint/css-hint.js"]))

(def default-options {:lineNumbers false
                      :matchBrackets true
                      :lineWrapping true
                      :styleActiveLine true
                      :tabMode "spaces"
                      :autofocus false
                      :extraKeys {"Ctrl-Space" "autocomplete"}
                      :theme "tomorrow-night-eighties"
                      :autoCloseBrackets true
                      :mode "css"})

(defn editor
  [value {:keys [style options on-init on-blur]}]
  (let [cm (ra/atom nil)]
    (ra/create-class
     {:component-did-mount
      (fn [this]
        (let [el (dom/dom-node this)]

          (reset! cm (.fromTextArea codemirror el (clj->js (merge default-options options))))

          (.setValue @cm value)

          (when on-blur
            (.on @cm "blur" #(on-blur (.getValue @cm))))

          (when on-init
            (on-init @cm))))

      :component-will-unmount
      (fn []
        (when @cm (reset! cm nil)))

      :reagent-render
      (fn [value]
        [:textarea {:value value :style style :on-blur #() :on-change #()}])})))
