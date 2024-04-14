<div align="center">

![Repath Studio](https://repath.studio/assets/images/banner.png)
<br>
 :construction: **This project is in alpha stage!**

[![GitHub Downloads (all assets, latest release)](https://img.shields.io/github/downloads/re-path/studio/latest/total?style=for-the-badge)](https://github.com/re-path/studio/releases/latest/)

[![Build desktop app](https://github.com/re-path/studio/actions/workflows/studio.yml/badge.svg)](https://github.com/re-path/studio/actions/workflows/studio.yml)
[![Deploy demo website](https://github.com/re-path/studio/actions/workflows/demo.yml/badge.svg)](https://github.com/re-path/studio/actions/workflows/demo.yml)
[![Outdated dependencies](https://github.com/re-path/studio/actions/workflows/dependencies.yml/badge.svg)](https://github.com/re-path/studio/actions/workflows/dependencies.yml)
[![Static security testing](https://github.com/re-path/studio/actions/workflows/clj-holmes.yml/badge.svg)](https://github.com/re-path/studio/actions/workflows/clj-holmes.yml)

</div>

![Studio Screenshot](https://repath.studio/assets/images/studio.png)

<!-- sponsors --><!-- sponsors -->

## Main goals

- Create a cross platform / open source vector graphics editor.
- Rely heavily on the [SVG](https://developer.mozilla.org/en-US/docs/Web/SVG) specification.
- Support [SMIL](https://developer.mozilla.org/en-US/docs/Web/SVG/SVG_animation_with_SMIL) animations - an extension of SVG allowing to animating SVG elements.
- Include an interactive REPL - a shell which allows you to evaluate clojure code to generate shapes or even extend the editor on the fly.
- Advanced undo/redo - maintain a history tree of all actions and never lose your redo stack.
- Implement built-in accessibility testing features.

## Rationale

### Why is this implemented as a web application?

- Using the main targeting platform to also create your SVGs, ensures that what you see while editing, is as close as possible to what you are going to get when you load your exported creations. 
- Avoid re-implementing complex specifications, like SMIL.
- Access to JavaScript ecosystem.
- Being able to serve this as website is a huge plus.

### Why is the desktop app wrapped with ElectronJS?

- Electron is a mature framework with a rich API. 
- Embedded Chromium ensures that web APIs will work consistently across multiple operating systems.
- Using the same rendering engine promotes UI consistency.  
- We can use the same language to develop our backend and frontend.

### Why ClojureScript?

- Built-in immutability.
- Easy data manipulation.
- Stability of libraries.
- Interoperability with js and react.
- Clean syntax.

### What about performance?

We are currently trying to optimize for hundreds of elements per document. Editing thousands of nodes on a single document is not within the scope of this project (at least for now).

## How to build it locally

### System Requirements
- [node.js](https://nodejs.org/)
- Java SDK (8+) [OpenJDK](https://openjdk.org/) or [Oracle](https://www.oracle.com/java/technologies/downloads/)
- [Clojure](https://clojure.org/guides/install_clojure)

Clone the project.
```
git clone https://github.com/re-path/studio.git
```
Go into the directory.
```
cd studio
```
Install the dependencies, build the app and watch the project files.
```
npm install && npm run dev
```
Run electron on a different terminal.
```
npm run electron
```
