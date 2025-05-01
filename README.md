<div align="center">

![Repath Studio](https://repath.studio/assets/images/banner.png)
<br>
 :construction: **This project is in alpha stage!**

[![GitHub Downloads (all assets, latest release)](https://img.shields.io/github/downloads/repath-project/repath-studio/latest/total?style=for-the-badge)](https://github.com/repath-project/repath-studio/releases/latest/)

[![Build desktop app](https://github.com/repath-project/repath-studio/actions/workflows/studio.yml/badge.svg)](https://github.com/repath-project/repath-studio/actions/workflows/studio.yml)
[![Deploy demo website](https://github.com/repath-project/repath-studio/actions/workflows/demo.yml/badge.svg)](https://github.com/repath-project/repath-studio/actions/workflows/demo.yml)
[![Outdated dependencies](https://github.com/repath-project/repath-studio/actions/workflows/dependencies.yml/badge.svg)](https://github.com/repath-project/repath-studio/actions/workflows/dependencies.yml)
[![Static security testing](https://github.com/repath-project/repath-studio/actions/workflows/clj-holmes.yml/badge.svg)](https://github.com/repath-project/repath-studio/actions/workflows/clj-holmes.yml)

</div>

![Studio Screenshot](https://repath.studio/assets/images/studio.png)

Repath Studio is a cross platform vector graphics editor, that combines procedural tooling with traditional design workflows. It includes an interactive shell, which allows evaluating code to generate shapes, or even extend the editor on the fly. Supporting multiple programming languages and enriching the existing API is planned. The tool relies heavily on the [SVG](https://developer.mozilla.org/en-US/docs/Web/SVG) specification, and aims to educate users about it. Creating and editing [SMIL](https://developer.mozilla.org/en-US/docs/Web/SVG/SVG_animation_with_SMIL) animations - an SVG extension – is an important aspect of the project, that is yet to be fully implemented. An advanced undo/redo mechanism is used to maintain a full history tree of actions in memory, so users will never lose their redo stack.

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

We are currently trying to optimize for hundreds of elements per document. We are also testing a canvas based implementation that can handle thousands of nodes on a single document, but that's not within the current scope of the project.

## How to build it locally

### System Requirements
- [node.js](https://nodejs.org/)
- Java SDK (8+) [OpenJDK](https://openjdk.org/) or [Oracle](https://www.oracle.com/java/technologies/downloads/)
- [Clojure](https://clojure.org/guides/install_clojure)

### Shell instructions

Clone the project.
```
git clone https://github.com/repath-project/repath-studio.git
```
Go into the directory.
```
cd repath-studio
```
Install the dependencies, build the app and watch the project files.
```
npm install && npm run dev
```
Run electron on a different terminal.
```
npm run electron
```

## Sponsors


<section data-markdown>
    <div>
        <a href="https://nlnet.nl/project/RepathStudio/"><img src="https://nlnet.nl/logo/banner.svg" alt="Logo NLnet: abstract logo of four people seen from above" width="200px"></a>
        &nbsp;&nbsp;
        <a href="https://nlnet.nl/core"><img src="https://nlnet.nl/image/logos/NGI0Core_tag.svg" alt="Logo NGI Zero: letterlogo shaped like a tag" width="250px"></a>
    </div>
</section>

<!-- sponsors --><!-- sponsors -->
