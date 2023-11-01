# Contributing

Thank you for your interest in actively participating in the project's development!
Please read the [Contributor Covenant Code of Conduct](https://github.com/re-path/studio/blob/main/CODE_OF_CONDUCT.md) 
and the [Contributor License Agreement](cla.md) first.

The project is based on [re-frame](https://github.com/day8/re-frame/), 
a framework for building Modern Web Apps in ClojureScript.
You should probably take a look at their [exceptional documentation](https://day8.github.io/re-frame/re-frame/).

## App structure

Main structure
<pre>
src\
├── renderer\     <--- <a href="https://www.electronjs.org/docs/latest/tutorial/process-model#the-renderer-process">renderer process</a>
├── main.cljs     <--- <a href="https://www.electronjs.org/docs/latest/tutorial/process-model#the-main-process">main process</a>
└── preload.cljs  <--- <a href="https://www.electronjs.org/docs/latest/tutorial/process-model#preload-scripts">preload script</a>
</pre>

We are trying to split our code under renderer into relatively independent modules, 
following [re-frame's app structure suggestions](https://day8.github.io/re-frame/App-Structure/)
with some minor additions. 

<pre>
module\
├── core.cljs     <--- entry point
├── db.cljs       <--- schema, validation
├── views.cljs    <--- reagent views
├── events.cljs   <--- event handlers
├── effects.cljs  <--- effectful handlers
├── subs.cljs     <--- subscription handlers
├── handlers.cljs <--- helper functions for db transformations
└── styles.css    <--- styles
</pre>

## How to build it locally

### System Requirements
- [node.js](https://nodejs.org/)
- Java SDK (8+) [OpenJDK](https://www.oracle.com/java/technologies/downloads/) or [Oracle](https://nodejs.org/)

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

### Useful development shortcuts

```
Ctrl+Shift+I Toggle devtools
Ctrl+H Toggle 10x
Ctrl+R Reload app
```