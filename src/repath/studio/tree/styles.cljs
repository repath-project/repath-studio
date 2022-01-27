(ns repath.studio.tree.styles
  (:require [stylefy.core :as stylefy]
            [repath.studio.styles :as styles]))


(stylefy/class "list-item-button" {:visibility "hidden"})

(stylefy/class "list-item" {:elements/align-items "center"
                            :cursor      "pointer"
                            :padding     "4px 11px"
                            :box-sizing  "border-box"
                            ::stylefy/manual [[:&:hover [:.list-item-button
                                                         {:visibility "visible"}]]
                                              [:button {:height "24px"
                                                        :width "24px"}]]})

(stylefy/class "collapse-button" {::stylefy/manual [[:svg {:height "10px"
                                                           :width "10px"}]]})

(stylefy/class "page-item" {:border-left (str "4px solid transparent")})

(stylefy/class "active" {:border-left-color styles/accent})

(stylefy/class "tree-heading" {:padding "4px 11px 4px 0"
                               :display "flex"
                               :text-align "left"
                               :font-size "11px"
                               :elements/align-items "center"
                               :text-transform "uppercase"
                               :font-weight "700"
                               ::stylefy/mode  {:hover  {:color            styles/font-color-hovered
                                                         :background-color "inherit"}
                                                :active {:background-color "inherit"}}
                               ::stylefy/manual [[:button {:height "18px"
                                                           :width "18px"}]]})

(stylefy/class "list-item-input" {:flex "1 1 auto"
                                  :background "transparent"
                                  :font-family "inherit"
                                  ::stylefy/mode {:placeholder {:color (styles/->!important styles/font-color)}}})

(stylefy/class "list-item-input::placeholder" {:color styles/font-color
                                               ::stylefy/mode {:focus {:color "red"}}})

(stylefy/class "list-item-input:focus::placeholder" {:color styles/font-color-muted})