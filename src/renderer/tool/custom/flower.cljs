(ns renderer.tool.custom.flower
  "Ported from
   https://gist.github.com/Askarizadeh986/2bb64fe3a134236fe6ff30875cf81d00
   License https://codepen.io/license/pen/XGLZLp"
  (:require
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :flower ::tool.hierarchy/custom)
