(ns repath.studio.context-menu.styles
  (:require [stylefy.core :as stylefy]
            [repath.studio.styles :as styles]))


(stylefy/class "context-menu-backdrop" {:background "transparent"
                                        :position :fixed
                                        :top 0
                                        :left 0
                                        :width "100vw"
                                        :height "100vh"
                                        :z-index 200})

(stylefy/class "context-menu" {:list-style "none"
                               :min-width "200px"
                               :background styles/level-1
                               :padding "4px 0"
                               :box-shadow "4px 4px 2px rgba(0, 0, 0, .4)"
                               ::stylefy/mode  {:focus {:outline "none"}}})


(stylefy/class "command-row" {:display "flex"
                              :padding styles/padding
                              :padding-left "32px"
                              :box-sizing "border-box"
                              :width "100%"
                              ::stylefy/mode     {:hover  {:cursor           "pointer"
                                                           :background-color styles/level-2}
                                                  :active {:background-color styles/level-3}}})


(stylefy/class "context-button" {:width "100%"})


(stylefy/class "context-submenu" {:position "relative"})

