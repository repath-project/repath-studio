(ns renderer.a11y.db
  (:require
   [renderer.i18n.db :refer [Translation]]))

(def A11yFilterId keyword?)

(def FilterTag
  [:enum
   :feSpotLight
   :feBlend
   :feColorMatrix
   :feComponentTransfer
   :feComposite
   :feConvolveMatrix
   :feDiffuseLighting
   :feDisplacementMap
   :feDropShadow
   :feFlood
   :feGaussianBlur
   :feImage
   :feMerge
   :feMorphology
   :feOffset
   :feSpecularLighting
   :feTile
   :feTurbulence])

(def A11yFilter
  [:map {:closed true}
   [:id A11yFilterId]
   [:tag FilterTag]
   [:label Translation]
   [:attrs {:default {}} [:map-of keyword? any?]]])

(def default
  [{:id :blur
    :tag :feGaussianBlur
    :label [::blur "blur"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :stdDeviation "1"}}

   {:id :blur-x2
    :tag :feGaussianBlur
    :label [::blur-x2 "blur-x2"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :stdDeviation "2"}}

   ;; https://github.com/hail2u/color-blindness-emulation

   {:id :protanopia
    :tag :feColorMatrix
    :label [::protanopia "protanopia"]
    :attrs {:in "SourceGraphic"
            :type "matrix"
            :value [0.567, 0.433, 0, 0, 0
                    0.558, 0.442, 0, 0, 0
                    0, 0.242, 0.758, 0, 0
                    0, 0, 0, 1, 0]}}

   {:id :protanomaly
    :tag :feColorMatrix
    :label [::protanomaly "protanomaly"]
    :attrs {:values [0.817, 0.183, 0, 0, 0
                     0.333, 0.667, 0, 0, 0
                     0, 0.125, 0.875, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :deuteranopia
    :tag :feColorMatrix
    :label [::deuteranopia "deuteranopia"]
    :attrs {:values [0.625, 0.375, 0, 0, 0
                     0.7, 0.3, 0, 0, 0
                     0, 0.3, 0.7, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :deuteranomaly
    :tag :feColorMatrix
    :label [::deuteranomaly "deuteranomaly"]
    :attrs {:values [0.8, 0.2, 0, 0, 0
                     0.258, 0.742, 0, 0, 0
                     0, 0.142, 0.858, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :tritanopia
    :tag :feColorMatrix
    :label [::tritanopia "tritanopia"]
    :attrs {:values [0.95, 0.05, 0, 0, 0
                     0, 0.433, 0.567, 0, 0
                     0, 0.475, 0.525, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :tritanomaly
    :tag :feColorMatrix
    :label [::tritanomaly "tritanomaly"]
    :attrs {:values [0.967, 0.033, 0, 0, 0
                     0, 0.733, 0.267, 0, 0
                     0, 0.183, 0.817, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :achromatopsia
    :tag :feColorMatrix
    :label [::achromatopsia "achromatopsia"]
    :attrs {:values [0.299, 0.587, 0.114, 0, 0
                     0.299, 0.587, 0.114, 0, 0
                     0.299, 0.587, 0.114, 0, 0
                     0, 0, 0, 1, 0]}}

   {:id :achromatomaly
    :tag :feColorMatrix
    :label [::achromatomaly "achromatomaly"]
    :attrs {:values [0.618, 0.320, 0.062, 0, 0
                     0.163, 0.775, 0.062, 0, 0
                     0.163, 0.320, 0.516, 0, 0
                     0, 0, 0, 1, 0]}}])
