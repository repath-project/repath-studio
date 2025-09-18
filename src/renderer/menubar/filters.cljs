(ns renderer.menubar.filters
  (:require
   [renderer.utils.i18n :refer [t]]))

(defn accessibility
  []
  [{:id :blur
    :tag :feGaussianBlur
    :label (t [::blur "blur"])
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :stdDeviation "1"}}

   {:id :blur-x2
    :tag :feGaussianBlur
    :label (t [::blur-x2 "blur-x2"])
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :stdDeviation "2"}}

   ;; https://github.com/hail2u/color-blindness-emulation
   {:id :protanopia
    :tag :feColorMatrix
    :label (t [::protanopia "protanopia"])
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :value [0.567, 0.433, 0, 0, 0
                    0.558, 0.442, 0, 0, 0
                    0, 0.242, 0.758, 0, 0
                    0, 0, 0, 1, 0]}}

   {:id :protanomaly
    :tag :feColorMatrix
    :label (t [::protanomaly "protanomaly"])
    :attrs {:values [0.817, 0.183, 0, 0, 0
                     0.333, 0.667, 0, 0, 0
                     0, 0.125, 0.875, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :deuteranopia
    :tag :feColorMatrix
    :label (t [::deuteranopia "deuteranopia"])
    :attrs {:values [0.625, 0.375, 0, 0, 0
                     0.7, 0.3, 0, 0, 0
                     0, 0.3, 0.7, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :deuteranomaly
    :tag :feColorMatrix
    :label (t [::deuteranomaly "deuteranomaly"])
    :attrs {:values [0.8, 0.2, 0, 0, 0
                     0.258, 0.742, 0, 0, 0
                     0, 0.142, 0.858, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :tritanopia
    :tag :feColorMatrix
    :label (t [::tritanopia "tritanopia"])
    :attrs {:values [0.95, 0.05, 0, 0, 0
                     0, 0.433, 0.567, 0, 0
                     0, 0.475, 0.525, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :tritanomaly
    :tag :feColorMatrix
    :label (t [::tritanomaly "tritanomaly"])
    :attrs {:values [0.967, 0.033, 0, 0, 0
                     0, 0.733, 0.267, 0, 0
                     0, 0.183, 0.817, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :achromatopsia
    :tag :feColorMatrix
    :label (t [::tritanomaly "tritanomaly"])
    :attrs {:values [0.299, 0.587, 0.114, 0, 0
                     0.299, 0.587, 0.114, 0, 0
                     0.299, 0.587, 0.114, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :achromatomaly
    :tag :feColorMatrix
    :label (t [::achromatopsia "achromatopsia"])
    :attrs {:values [0.618, 0.320, 0.062, 0, 0
                     0.163, 0.775, 0.062, 0, 0
                     0.163, 0.320, 0.516, 0, 0
                     0, 0, 0, 1, 0]}}])

(def A11yFilter
  [:enum :blur :blur-x2 :protanopia :protanomaly :deuteranopia :deuteranomaly :tritanopia
   :tritanomaly :achromatopsia :achromatomaly])
