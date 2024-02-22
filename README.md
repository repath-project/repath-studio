<div align="center">

![Repath Studio](https://repath.studio/assets/images/banner.png)\
 :construction: **This project is in alpha stage!**

[![Discord](https://img.shields.io/discord/890005586958237716?color=%235865F2&label=Discord&logo=discord&logoColor=%23aaaaaa)](https://discord.gg/yzjY6W6ame)
![X (formerly Twitter) Follow](https://img.shields.io/twitter/follow/repath_studio?style=flat)
<br>
[![CodeScene Code Health](https://codescene.io/projects/47852/status-badges/code-health)](https://codescene.io/projects/47852)
[![Build desktop app](https://github.com/re-path/studio/actions/workflows/studio.yml/badge.svg)](https://github.com/re-path/studio/actions/workflows/studio.yml)
[![Deploy demo website](https://github.com/re-path/studio/actions/workflows/demo.yml/badge.svg)](https://github.com/re-path/studio/actions/workflows/demo.yml)
[![Outdated dependencies](https://github.com/re-path/studio/actions/workflows/dependencies.yml/badge.svg)](https://github.com/re-path/studio/actions/workflows/dependencies.yml)
[![Static security testing](https://github.com/re-path/studio/actions/workflows/clj-holmes.yml/badge.svg)](https://github.com/re-path/studio/actions/workflows/clj-holmes.yml)

</div>

![Studio Screenshot](https://repath.studio/assets/images/studio.png)

## Main goals

- Create a cross platform / open source vector graphics editor.
- Rely heavily on the [SVG](https://developer.mozilla.org/en-US/docs/Web/SVG) specification.
- Support [SMIL](https://developer.mozilla.org/en-US/docs/Web/SVG/SVG_animation_with_SMIL) animations - an extension of SVG allowing to animating SVG elements.
- Include an interactive REPL - a shell which allows you to evaluate clojure code to generate shapes or even extend the editor on the fly.
- Advanced undo/redo - maintain a history tree of all actions and never lose your redo stack.
- Implement built-in accessibility testing features.
