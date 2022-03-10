(ns repath.studio.styles
  (:require [stylefy.core :as stylefy]
            [clojure.string :as str]))

(defn square-styles [size]
  {:width       size
   :height      size
   :line-height size})

(defn ->!important
  [value]
  (str value " !important"))

;; Load Source Sans Pro font

(stylefy/font-face {:font-family "Source Sans Pro"
                    :src "url('fonts/Source_Sans_Pro/SourceSansPro-Light.ttf')"
                    :font-weight "300"
                    :font-style "normal"})

(stylefy/font-face {:font-family "Source Sans Pro"
                    :src "url('fonts/Source_Sans_Pro/SourceSansPro-Regular.ttf')"
                    :font-weight "400"
                    :font-style "normal"})

(stylefy/font-face {:font-family "Source Sans Pro"
                    :src "url('fonts/Source_Sans_Pro/SourceSansPro-SemiBold.ttf')"
                    :font-weight "500"
                    :font-style "normal"})

(stylefy/font-face {:font-family "Source Sans Pro"
                    :src "url('fonts/Source_Sans_Pro/SourceSansPro-Bold.ttf')"
                    :font-weight "600"
                    :font-style "normal"})

;; Load Source Code Pro font

(stylefy/font-face {:font-family "Source Code Pro"
                    :src "url('fonts/Source_Code_Pro/SourceCodePro-Light.ttf')"
                    :font-weight "light"
                    :font-style "light"})

(stylefy/font-face {:font-family "Source Code Pro"
                    :src "url('fonts/Source_Code_Pro/SourceCodePro-Regular.ttf')"
                    :font-weight "normal"
                    :font-style "regular"})

(stylefy/font-face {:font-family "Source Code Pro"
                    :src "url('fonts/Source_Code_Pro/SourceCodePro-SemiBold.ttf')"
                    :font-weight "semi-bold"
                    :font-style "semibold"})

(stylefy/font-face {:font-family "Source Code Pro"
                    :src "url('fonts/Source_Code_Pro/SourceCodePro-Bold.ttf')"
                    :font-weight "bold"
                    :font-style "bold"})

(def font-family "Source Sans Pro")
(def font-family-mono "Source Code Pro")

;; SEE https://www.nordtheme.com/docs/colors-and-palettes

;; (def level-0 "#2E3440")
;; (def level-1 "#3B4252")
;; (def level-2 "#434C5E")
;; (def level-3 "#4C566A")

;; (def active "#4C566A")

;; (def accent "#88C0D0")

;; (def font-color "#E5E9F0")
;; (def error-color "#BF616A")
;; (def font-color-disabled "rgba(255, 255, 255, .3)")
;; (def font-color-muted "##D8DEE9")
;; (def font-color-hovered "#ECEFF4")
;; (def font-color-active "rgba(255, 255, 255, 1)")

;; (def border-color "rgba(255, 255, 255, .15)")

;; (def background-error "#BF616A")
;; (def background-warning "#D08770")
;; (def background-success "#A3BE8C")


;; Light Theme

;; (def level-0 "#d1d1d1")
;; (def level-1 "#e1e1e1")
;; (def level-2 "#eee")
;; (def level-3 "#fff")

;; (def active "#fff")

;; (def accent "#ec407a")

;; (def font-color "#rgba(0, 0, 0, .8)")
;; (def error-color "#f78484")
;; (def font-color-disabled "rgba(0, 0, 0, .4)")
;; (def font-color-muted "rgba(0, 0, 0, .6)")
;; (def font-color-hovered "#rgba(0, 0, 0, .95)")
;; (def font-color-active "rgba(0, 0, 0, 1)")

;; (def border-color "rgba(0, 0, 0, .15)")

;; (def background-error "#962121")
;; (def background-warning "#a56a00")
;; (def background-success "#1c611c")



(def level-0 "#212121")
(def level-1 "#282828")
(def level-2 "#313131")
(def level-3 "#414141")

(def active "#414141")

(def accent "#ec407a")

(def font-color "rgba(255, 255, 255, .7)")
(def error-color "#f78484")
(def font-color-disabled "rgba(255, 255, 255, .3)")
(def font-color-muted "rgba(255, 255, 255, .5)")
(def font-color-hovered "rgba(255, 255, 255, .9)")
(def font-color-active "rgba(255, 255, 255, 1)")

(def border-color "rgba(255, 255, 255, .15)")

(def background-error "#962121")
(def background-warning "#a56a00")
(def background-success "#1c611c")


(stylefy/class "error" {:background background-error})
(stylefy/class "warning" {:background background-warning})
(stylefy/class "success" {:background background-success})

(def h-padding "12px")
(def v-padding "8px")
(def padding (str v-padding " " h-padding))

(def form-element-style
  {:outline "none"
   :background level-2
   :border 0
   :padding "4px 12px"
   :box-sizing "border-box"
   :line-height "18px"
   :width "100%"
   :font-size "12px"
   :box-shadow (->!important "none")
   :color (->!important font-color)
   ::stylefy/mode {:focus {:border-color font-color-muted}}})

(stylefy/tag "body" {:margin      0
                     :padding     0
                     :background  (->!important level-0)
                     :overflow    "hidden"
                     :font-family font-family
                     :font-size   "13px"
                     :color       font-color
                     :fill        font-color})

(stylefy/tag "::-webkit-scrollbar" {:width  "10px"
                                    :height "10px"})
(stylefy/tag "::-webkit-scrollbar-thumb" {:background level-3})
(stylefy/tag "::-webkit-scrollbar-corner" {:background level-3})
(stylefy/tag "::-webkit-scrollbar-track" {:background "transparent"})

(stylefy/tag "input[type=range]" {:-webkit-appearance "none"
                                  :padding "5px"
                                  :margin 0
                                  :background level-2
                                  ::stylefy/mode [[:focus {:outline "none"}]
                                                  [:hover {:cursor "col-resize"}]]})

(stylefy/tag "input[type=range]::-webkit-slider-runnable-track" {:height "16px"
                                                                 :background level-1
                                                                 :border "0"
                                                                 :border-radius 0})
(stylefy/tag "input[type=range]::-webkit-slider-thumb" {:-webkit-appearance "none"
                                                        :border "none"
                                                        :height "16px"
                                                        :width "4px"
                                                        :border-radius 0
                                                        :background font-color
                                                        :margin-top "0"})

(stylefy/class "v-scroll" {:overflow "hidden"
                           :box-sizing "border-box"
                           ::stylefy/mode {:hover {:overflow-y "auto"}}})

(def button-styles {:background-color "transparent"
                    :border           0
                    :padding          0
                    :color            font-color
                    :fill             font-color
                    :font-family      font-family
                    :font-size        "1em"
                    :outline          "none"
                    :cursor           "pointer"
                    :position         "relative"
                    :-webkit-app-region "no-drag"
                    ::stylefy/mode     {:hover  {:cursor "pointer"
                                                 :color font-color-active
                                                 :fill font-color-active}
                                        :active {:background-color level-3}}})

(def flex-box {:display         "flex"
               :overflow       "hidden"})

(stylefy/class "v-box" (merge flex-box {:flex-direction "column"}))

(stylefy/class "h-box" flex-box)

(stylefy/tag "a" {:color accent
                  :cursor "pointer"
                  :text-decoration "none"})

(stylefy/class "tooltip" {:position "absolute"
                          :background-color level-3
                          :border-radius "4px"
                          :z-index 1
                          :margin "8px"
                          :line-height "24px"
                          :padding "0 8px"})

(stylefy/tag "button" button-styles)
(stylefy/class "button" button-styles)

(stylefy/class "divider-v" {:margin      "0 4px"
                            :border-left (str "1px solid " border-color)
                            :height      "28px"})

(stylefy/class "divider" {:margin      "4px 0"
                          :border-bottom (str "1px solid " border-color)})

(stylefy/class "selected" {:background-color (->!important active)})

(stylefy/class "muted" {:color font-color-muted})

(stylefy/class "disabled" {:opacity ".5"
                           :cursor (->!important "initial")
                           :pointer-events "none"
                           ::stylefy/mode {:hover {:background-color (->!important "inherit")}}})

(stylefy/class "icon" {:display "flex"
                       :justify-content "center"
                       ::stylefy/manual [[:div {:display "flex"
                                                :fill "inherit"}]]})

(stylefy/class "icon-button" (merge button-styles
                                    (square-styles "32px")
                                    {:margin "2px"
                                     :border-radius "4px"
                                     ::stylefy/mode  {:hover  {:color font-color-active}
                                                      :disabled  {:background "transparent"}
                                                      :active {:color font-color-active
                                                               :background level-3}}}))

(stylefy/class "v-devider" {:margin      "4px"
                            :border-left (str "1px solid " border-color)
                            :height      "28px"})

(stylefy/class "h-devider" {:margin     "4px"
                            :border-top (str "1px solid " border-color)
                            :width      "28px"})

(stylefy/class "sidebar" {:flex   "0 0 auto"})

(stylefy/class "shortcut" {:flex   "1 0"
                           :text-align "right"})

(def color-picker-styles {:background (->!important "transparent")
                          :box-shadow (->!important "none")
                          :color (->!important font-color)
                          ::stylefy/manual [[:div {:border-width (->!important "0")
                                                   :color (->!important font-color)}]
                                            [:svg {:fill (->!important font-color)
                                                   :background (->!important "transparent")}]]})

(stylefy/class "chrome-picker" color-picker-styles)
(stylefy/class "photoshop-picker" color-picker-styles)

(stylefy/class "color-rect" {:width        "32px"
                             :height       "32px"
                             :position     "absolute"
                             :cursor       "pointer"
                             :box-sizing   "border-box"
                             :border-width "1px"
                             :border-style "solid"
                             :border-color "#aaa"})

(stylefy/class "color-drip" {:width  "16px"
                             :height "16px"
                             :margin-left "1px"
                             :cursor "pointer"})

(stylefy/class "command-palette" {:width "200px"
                                  :position "absolute"
                                  :top "20px"
                                  :left "calc(50% - 100px)"})
