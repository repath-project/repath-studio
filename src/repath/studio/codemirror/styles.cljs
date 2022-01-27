(ns repath.studio.codemirror.styles
  (:require [stylefy.core :as stylefy]
            [repath.studio.styles :as styles]))

(stylefy/class "CodeMirror-selected" {:background (styles/->!important styles/level-3)})

(stylefy/class "CodeMirror-cursor" {:border-left (styles/->!important (str "1px solid " styles/font-color))
                                    :height (styles/->!important "16px")})
(stylefy/class "CodeMirror-matchingbracket" {:background (styles/->!important styles/level-3)
                                             :text-decoration (styles/->!important "none")})
(stylefy/class "CodeMirror-wrap" (merge styles/form-element-style {:rows "5"
                                                                   :padding 0
                                                                   ::stylefy/manual [[:pre {:word-break (styles/->!important "break-word")}]]}))


(stylefy/class "CodeMirror-gutters" {:background  (styles/->!important styles/level-0)})
(stylefy/class "CodeMirror" {:background (styles/->!important "transparent")
                             :z-index "0"
                             :height "auto"
                             :font-family  "Source Code Pro, monospace"})