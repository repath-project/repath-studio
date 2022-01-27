(ns repath.studio.window.styles
  (:require [stylefy.core :as stylefy]
            [repath.studio.styles :as styles]))

(stylefy/class "window-control-button" {:padding "7px 14px"})

(stylefy/class "title-bar" {:flex               "1 1 100%"
                            :color              "#888"
                            :justify-content    "center"
                            :padding            styles/padding
                            :-webkit-app-region "drag"})