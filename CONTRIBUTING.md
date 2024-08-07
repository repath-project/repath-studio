# Contributing

Thank you for your interest in actively participating in the project's development!
Please read the [Contributor Covenant Code of Conduct](https://github.com/repath-project/repath-studio/blob/main/CODE_OF_CONDUCT.md)
and the [Contributor License Agreement](cla.md) first.

The project is based on [re-frame](https://github.com/day8/re-frame/),
a framework for building Modern Web Apps in ClojureScript.
You should probably take a look at their [exceptional documentation](https://day8.github.io/re-frame/re-frame/).

## Style Guide

We try to follow the [Clojure Style Guide](https://guide.clojure.style/) as much as possible.

In addition to the [idiomatic names](https://guide.clojure.style/#idiomatic-names),
we use the following conventions

<pre>
e           -> event
el, els     -> element, elements
attr, attrs -> attribute, attributes
prop, props -> property, properties
</pre>

We also use the following namespace aliases
<pre>
v  -> views
e  -> events
h  -> handlers
s  -> subs
fx -> effects
</pre>

If the namespace belongs to a dedicated module, we use `module.v`.

## App structure

Main structure
<pre>
src\
├── renderer\     <--- <a href ="https://www.electronjs.org/docs/latest/tutorial/process-model#the-renderer-process">Renderer Process</a>
├── electron\     <--- <a href ="https://www.electronjs.org/docs/latest/tutorial/process-model#the-main-process">Main Process</a> & <a href="https://www.electronjs.org/docs/latest/tutorial/process-model#preload-scripts">Preload script</a>
├── lang\         <--- Translation files
└── worker\       <--- <a href ="https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API">Web Workers</a>
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
├── subs.cljs     <--- subscription handlers
├── handlers.cljs <--- helper functions for db transformations
├── effects.cljs  <--- effect handlers
├── styles.css    <--- styles
└── README.md     <--- documentation
</pre>

## Useful development shortcuts

```
Ctrl+Shift+I Toggle devtools
Ctrl+Shift+X Toggle 10x
Ctrl+R Reload app
```
