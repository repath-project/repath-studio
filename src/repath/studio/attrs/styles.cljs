(ns repath.studio.attrs.styles
  (:require [stylefy.core :as stylefy]
            [repath.studio.styles :as styles]))


(stylefy/tag "input" (merge styles/form-element-style {:font-family "Source Code Pro, monospace"}))

(stylefy/tag "textarea" (merge styles/form-element-style {:width "100%"
                                                          :resize "none"}))

(stylefy/class "form-group" {:vertical-align "top"
                             :background styles/level-2
                             ::stylefy/manual [[:td
                                                {:padding 0}]]})

(stylefy/tag "label" (merge styles/form-element-style {:display "block"
                                                       :text-align "right"
                                                       :text-overflow "ellipsis"
                                                       :overflow "hidden"
                                                       :height "100%"
                                                       :width "auto"
                                                       :color styles/font-color-muted
                                                       ::stylefy/mode {:hover {:color styles/font-color-hovered
                                                                               :cursor "pointer"}}}))

(stylefy/tag "dl" {:margin 0})

(stylefy/tag "dt" (merge styles/form-element-style {:background styles/level-2}))
(stylefy/tag "dd" {:margi-left 0
                   :margin-inline-start 0
                   :padding 0})

(stylefy/tag "select" (merge styles/form-element-style {:flex "0 1 90px"
                                                        :margin-left "1px"
                                                        :text-align "left"
                                                        :padding "8px"
                                                        :background styles/level-2
                                                        :color styles/font-color-muted
                                                        ::stylefy/mode {:hover {:color styles/font-color-hovered
                                                                                :cursor "pointer"}}}))

(stylefy/tag "h2" {:font-weight "100"})

(stylefy/class "support-cell" {:font-size "10px" :padding "1px" :margin "1px"})
