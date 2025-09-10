# Frame

This is an iFrame element that hosts anything that needs to be rendered.
The main SVG canvas is rendered inside the iFrame using
[react-frame-component](https://github.com/ryanseddon/react-frame-component).

## Why would you render to an iFrame?

### Style encapsulation

We won't have to worry about style inheritance.

### Layout boundaries 

Canvas elements will not affect the parent window.

## Caveats
The iFrame introduces a different browsing context so we need to simulate 
some of the events to the parent window. We also have to redeclare some styles,
because we can't use the css variables of the parent window.
