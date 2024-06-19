# Changelog

## 0.2.11 (2024-06-19)

**Removed**

- Image triangulation

## 0.2.10 (2024-06-19)

**Added**

- Various icons

**Removed**

- kibit development dependency

**Changed**

- Project name
- Simplify light theme

**Fixed**

- Selection on animate
- Paste in place selection
- Parent of pasted elements
- Saved document subs
- Popover arrow
- Attribute grid styles

## 0.2.9 (2024-04-22)

**Added**

- Image tracing
- Image triangulation
- SVG import
- Snap options
- Path manipulations (simplify, smooth, flatten, reverse)
- Brush circle pointer
- Open document on drop
- Various icons
- Web workers and loading indicator

**Removed**

- Element-to-path dependency
- Tooltip arrow

**Changed**

- Allow selecting svg elements on drag
- Enable chromium devtools on prod
- Enhance focus styles
- Rearrange toolbars and menus
- Move tool help text to statusbar
- Enhance a11y
- Enhance performance

**Fixed**

- Set-zoom event
- Render to string
- Default modal focus
- Text element creation
- Point parsing
- Prevent copying empty selection
- Recent documents on home

## 0.2.8 (2024-04-09)

**Added**

- Snapping to points
- Image drop
- Export for browsers
- Open and download file for legacy browsers
- Multi-select on file open

**Changed**

- File association
- Enhanced error notifications and dialogs
- Replaced shortcuts with command dialog
- Remove ctrl from multiselect

**Fixed**

- Tree item double click area
- Attribute order
- Prevent default on canvas

## 0.2.7 (2024-03-15)

**Fixed**

- Image sources
- Shortcuts
- Git version

## 0.2.6 (2024-03-15)

**Added**

- Keyboard shortcuts dialog
- About dialog
- Close and save confirmation dialog
- Command button to menubar
- Path editing (wip)

**Changed**

- Enhance command dialog
- Refine shortcuts
- Refactor toolbar styles
- Enhance popovers

**Fixed**

- Menu order
- Dot icon

## 0.2.5 (2024-03-05)

**Added**

- Recent documents to home page
- close saved and containing directory actions to document menu
- Disabled state on context menu items
- Documents menu

**Changed**

- Rename frame events
- Enhance slider
- Enhance window controls
- Enhance file operations

**Fixed**

- Nil recent paths
- App icon margin
- Undo/redo dom structure
- Select content max height
- Setting parent of non-existing element
- Missing default element attrs
- Window keyboard listeners

## 0.2.4 (2024-03-03)

**Added**

- Support save/load file on supported browsers
- Recent open documents
- Pull request template

**Changed**

- Enhance home page
- Enhance undo/redo styles

**Fixed**

- Document title

## 0.2.3 (2024-03-01)

**Fixed**

- Electron log
- Electron reloader
- Default panel size

## 0.2.2 (2024-03-01)

**Added**

- File save/load
- Auto reload on main changes

**Changed**

- Url handling
- Refactor element to path

**Fixed**

- History tree rendering
- Element selection

## 0.2.1 (2024-02-26)

**Changed**

- Logo

**Fixed**

- Resizable panels

## 0.2.0 (2024-02-22)

**Added**

- Timeline module
- History tree module
- Notification module
- Error boundary component
- Pressure-sensitive brush tool
- Select similar objects
- Multi-element resize on anchor point
- Theme switch
- Persist workspace configuration
- Introduce centroid (wip)
- Translation module (wip)

**Removed**

- Page element
- Most google closure deps
- Fluentui dependency

**Changed**

- UI rewrite
- Extended refactoring
- Enhance all tools
- Enhance menubar
- Enhance user repl commands

**Fixed**

Too many fixes to list.

## 0.1.3 (2022-05-09)

**Added**

- deps.edn to add de-dupe dependency
- matrix chat to README.md
- new page button action
- overlay color
- element selection to document history
- history icon and button
- XML view and code icon
- double-click multimethod
- select drag-end default case
- shape functions to user namespace
- edit multimethod and state
- select-box component
- multi-element scaling support

**Changed**

- history select background
- xml and history moved under documents
- package upgrades
- page element and render to string method
- radius calculation
- attribute module refactoring
- dropper tool refactoring
- rotate, history, ungroup icons
- cancel event enhanced
- maintain default state on zoom, pan, ruler
- banner image
- bound operations refactoring
- scale multimethod
- move state renamed
- zoom tool enhanced

**Removed**

- selected method to fix a build error
- mouse over canvas flag
- xml-formatter dependency
- mouse click simulation from canvas

**Fixed**

- default background value of page
- render to string method
- update codemirror on value change
- export to svg
- bounds and size visibility
- align method
- select tool

## 0.1.2 (2022-04-09)

**Added**

- out of canvas mouse tracking and and auto-panning
- active document interceptor
- clj-holmes.yml
- CONTRIBUTING.md
- help function to user namespace
- animation icons

**Changed**

- events and subs
- bcd svg spec
- bounds multimethod
- upgrade electron
- drag-end method of select tool
- move multimethod renamed to translate
- transform method simplified
- README.md

**Fixed**

- ellipse translate method

## 0.1.1 (2022-03-19)

**Added**

- document title to app header
- paste in position
- multiple commands to user namespace
- right click check
- ungroup event
- ruler tool
- fill keyboard shortcut
- keyboard shortcuts for multiple tools
- dropper tool

**Removed**

- reagent and re-frame from bootstrap entries
- re-frame events from repl completions
- path button

**Changed**

- import/export buttons disabled
- hide browser compatibility data
- mouse event simplified
- button styles
- dropper tool
- image tool
- packages upgraded

**Fixed**

- monospace font
- drag page method
- delete and ungroup methods
- shorcuts order
- no accessibility filter selection
- zoom validation


## 0.1.0 (2022-03-6)

**Added**

- Initial proof of concept.
