(ns repath.studio.theme.db
  (:require [repath.studio.styles :as styles]))

(defonce themes {:default {:palette {:accent "styles/accent" ; Color code for the accent.
                                     :black "black"  ; Color code for the strongest color, which is black in the default theme. This is a very light color in inverted themes.
                                     :neutralDark styles/font-color-hovered ; Color code for neutralDark.
                                     :neutralLight styles/level-3 ; Color code for neutralLight.
                                     :neutralLighter styles/border-color ; Color code for neutralLighter.
                                     :neutralLighterAlt "brown" ; Color code for neutralLighterAlt.
                                     :neutralPrimary styles/font-color ; Color code for neutralPrimary.
                                     :neutralPrimaryAlt "black" ; Color code for neutralPrimaryAlt.
                                     :neutralQuaternary "blue" ; Color code for neutralQuaternary.
                                     :neutralQuaternaryAlt styles/level-3 ; Color code for neutralQuaternaryAlt.
                                     :neutralSecondary styles/font-color-muted ; Color code for neutralSecondary.
                                     :neutralSecondaryAlt "black" ; Color code for neutralSecondaryAlt.
                                     :neutralTertiary "black" ; Color code for neutralTertiary.
                                     :neutralTertiaryAlt styles/font-color-disabled ; Color code for neutralTertiaryAlt.
                                     :themeDark styles/font-color ; Color code for themeDark.
                                     :themeDarkAlt styles/font-color ; Color code for themeDarkAlt.
                                     :themeDarker "brown" ; Color code for themeDarker.
                                     :themeLight "blue" ; Color code for themeLight.
                                     :themeLighter "pink" ; Color code for themeLighter.
                                     :themeLighterAlt "pink" ; Color code for themeLighterAlt.
                                     :themePrimary  styles/font-color-muted ; Color code for themePrimary.
                                     :themeSecondary styles/level-2 ; Color code for themeSecondary.
                                     :themeTertiary "yellow" ; Color code for themeTertiary.
                                     :white styles/level-0} ; Color code for the softest color, which is white in the default theme. This is a very dark color in dark themes. This is the page background.
                                     
                           :defaultFontStyle {:fontFamily styles/font-family
                                              :fontWeight "regular"}
                           :fonts {:small {:fontSize "10px"}
                                   :medium {:fontSize "12px"}
                                   :large {:fontSize "16px"
                                           :fontWeight "semibold"}
                                   :xLarge {:fontSize "20px"
                                            :fontWeight "semibold"}}}})
