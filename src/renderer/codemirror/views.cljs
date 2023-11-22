(ns renderer.codemirror.views
  (:require
   ["codemirror" :as codemirror]
   ["codemirror/addon/hint/css-hint.js"]
   ["codemirror/addon/hint/show-hint.js"]
   ["codemirror/mode/css/css.js"]
   ["codemirror/mode/xml/xml.js"]
   ["react" :as react]
   [reagent.core :as ra]))

(def default-options
  {:lineNumbers false
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
  (let [cm (ra/atom nil)
        ref (react/createRef)]
    (ra/create-class
     {:component-did-mount
      (fn [_this]
        (let [el (.-current ref)
              options (clj->js (merge default-options options))]

          (reset! cm (.fromTextArea codemirror el options))

          (.setValue @cm value)

          ;; Line up wrapped text with the base indentation.
          ;; SEE: https://codemirror.net/demo/indentwrap.html
          (.on @cm "renderLine" (fn [editor line elt]
                                  (let [off (* (.countColumn codemirror (.-text line) nil (.getOption editor "tabSize"))
                                               (.defaultCharWidth @cm))]
                                    (set! (.. elt -style -textIndent)
                                          (str "-" off "px"))
                                    (set! (.. elt -style -paddingLeft)
                                          (str (+ 4 off) "px")))))

          (.refresh @cm)

          (when on-blur
            (.on @cm "blur" #(on-blur (.getValue %))))

          (when on-init
            (on-init @cm))))

      :component-will-unmount
      (fn []
        (when @cm (reset! cm nil)))

      :component-did-update
      (fn [this _]
        (let [value (second (ra/argv this))]
          (.setValue @cm value)))

      :reagent-render
      (fn [value]
        [:textarea {:value value
                    :style style
                    :on-blur #()
                    :on-change #()
                    :ref ref}])})))
