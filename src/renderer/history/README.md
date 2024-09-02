# History  module

This module was originally inspired by [day8/re-frame-undo](https://github.com/day8/re-frame-undo) which provides a decent history implementation by utilizing the built-in immutability of the language.

## Why build something new?

- This library registers a single history for the app. We need a different history stack per document.
- The state is pushed into a ratom to avoid adding the history to the state. We need to store the history to our app-db.
- It is implemented using a the traditional undo and redo stacks. We need to preserve the whole tree.
- It uses an interceptor to store the changes of re-frame events which is limited in various ways.

See below for implementation details
https://github.com/day8/re-frame-undo/blob/master/src/day8/re_frame/undo.cljs

## What we currently do

- Each document has a dedicated `:history` map.
- We can also cancel an operation by swapping the current state with the last history point (see `renderer.history.handlers/swap`).
- Our state is normalized to easily access specific points in history. We can then look for a specific state on our tree by id and then retrieve its data with a simple `get-in`.
- All changes under [:documents :document-key :elements] should be considered undoable and part of the file history.

The end result on our db looks like this
```clojure
:history {:position :6
          :states {:907fdbc9-d2ad-4697-bea3-9aa5e2c46054 {...}
                   :ed0cce51-d00e-43e0-a06a-04c72ccce98f {:elements {...} ; Our actual state
                                                          :explanation "Set x to 500.3"
                                                          :id :ed0cce51-d00e-43e0-a06a-04c72ccce98f
                                                          :timestamp 1647882725718
                                                          :index 2
                                                          :parent :907fdbc9-d2ad-4697-bea3-9aa5e2c46054
                                                          :children [:cded6d1d-619f-4974-af95-8313cefd6f87]}
                   :cded6d1d-619f-4974-af95-8313cefd6f87 {...}}}
```
