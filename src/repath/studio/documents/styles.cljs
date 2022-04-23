(ns repath.studio.documents.styles
  (:require [stylefy.core :as stylefy]))

(stylefy/class "document-tab" {:elements/align-items     "center"
                               :padding         "8px 8px 8px 16px"
                               :flex            "0 1 130px"
                               :text-align      "left"
                               :margin-right    "1px"
                               ::stylefy/manual [[:&:hover [:.close-document-button
                                                            {:visibility "visible"}]]]})

(stylefy/class "document-name"  {:flex "1"
                                 :text-overflow "ellipsis"
                                 :overflow "hidden"
                                 :white-space "nowrap"
                                 :pointer-events "none"})

(stylefy/class "close-document-button" {:visibility "hidden"})
