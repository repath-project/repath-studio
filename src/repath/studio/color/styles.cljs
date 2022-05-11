(ns repath.studio.color.styles
  (:require [stylefy.core :as stylefy]
            [repath.studio.styles :as styles]))


(stylefy/class "palette" {:margin-left styles/h-padding
                          :flex "1"
                          :height (* styles/icon-size 2)
                          :margin "2px"})

(stylefy/class "picker" {:width    "48px"
                         :height   "48px"
                         :margin "2px"
                         :position "relative"})