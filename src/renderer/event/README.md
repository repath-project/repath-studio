# Event Module

The event module provides a layer of abstraction for handling canvas user interactions,
including pointer/keyboard/wheel events and drag-and-drop operations.

## Flow

Browser events are captured by implementation handlers. The default event behavior is 
prevented, and propagation is stopped. Native events are converted to Clojure data 
structures, spec'd using Malli schemas. Events are synchronously dispatched through
re-frame for responsive handling. Core business logic processes the events and updates the
application state. Events are delegated to the active tool via `renderer.tool.hierarchy`.
Tools implement their own event handlers for abstracted events, like `on-drag-start`.
