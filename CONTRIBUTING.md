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

- e - event
- el - element
- attr, attrs - attribute, attributes
- prop, props - property, properties

We also use the following namespace aliases

- v - views
- e - events
- h - handlers
- s - subs

If the namespace belongs to a different module, we use `module.v`.

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
├── subs.cljs     <--- subscription handlers
├── handlers.cljs <--- helper functions for db transformations
├── styles.css    <--- styles
└── README.md     <--- documentation
</pre>

## Useful development shortcuts

```
Ctrl+Shift+I Toggle devtools
Ctrl+Shift+H Toggle 10x
Ctrl+R Reload app
```
