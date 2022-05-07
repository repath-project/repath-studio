(ns repath.studio.tools.text
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]))

(derive :text ::tools/graphics)

(defmethod tools/properties :text [] {:icon "text"
                                      :description "The SVG <text> element draws a graphics element consisting of text. It's possible to apply a gradient, pattern, clipping path, mask, or filter to <text>, like any other SVG graphics element."
                                      :attrs [:content
                                              :font-family
                                              :font-size
                                              :font-weight
                                              :stroke
                                              :stroke-width
                                              :opacity]})

(defmethod tools/activate :text
  [db]
  (assoc db :cursor "text"))

(defmethod tools/mouse-up :text
  [{:keys [adjusted-mouse-offset] :as db}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        attrs {:x offset-x
               :y offset-y
               :fill "#000000"}]
    (elements/create db {:type :text :attrs attrs})))

(defmethod tools/drag :text
  [{:keys [adjusted-mouse-offset] :as db}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        attrs {:x offset-x
               :y offset-y
               :fill "#000000"}]
    (elements/set-temp db {:type :text :attrs attrs})))

(defmethod tools/bounds :text
  [element]
  (:bounds element))

(defmethod tools/path :text
  [{:keys [attrs]}]
    (.textToPath js/window.api
                 "/usr/share/fonts/liberation/LiberationSans-Regular.ttf"
                 (:content attrs)
                 (js/parseFloat (:x attrs))
                 (js/parseFloat (:y attrs))
                 (js/parseFloat (or (:font-size attrs) 16))))
    